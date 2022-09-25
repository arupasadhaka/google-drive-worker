package com.oslash.integration.manager;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.drive.model.File;
import com.oslash.integration.config.AppConfiguration;
import com.oslash.integration.models.FileMeta;
import com.oslash.integration.models.User;
import com.oslash.integration.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.integration.chunk.RemoteChunkingManagerStepBuilderFactory;
import org.springframework.batch.integration.partition.RemotePartitioningManagerStepBuilderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.integration.aws.outbound.SqsMessageHandler;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.json.ObjectToJsonTransformer;
import org.springframework.integration.support.json.Jackson2JsonObjectMapper;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Configuration
@Profile("manager")
public class ManagerConfiguration {
    private final Logger logger = LoggerFactory.getLogger(ManagerConfiguration.class);

    @Autowired
    private AppConfiguration appConfiguration;

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    public abstract class JobExecutionMixin {
        @JsonManagedReference
        private Collection<StepExecution> stepExecutions;
    }

    public abstract class StepExecutionsMixin {
        @JsonIgnore
        private JobExecution jobExecution;
    }

    @Bean
    public Jackson2ObjectMapperBuilder jacksonBuilder() {
        Jackson2ObjectMapperBuilder b = new Jackson2ObjectMapperBuilder();
        b.indentOutput(true)
        .mixIn(org.springframework.batch.core.StepExecution.class, StepExecutionsMixin.class)
        .mixIn(org.springframework.batch.core.JobExecution.class, JobExecutionMixin.class);
        return b;
    }

    /**
     *
     * issue: https://github.com/spring-projects/spring-batch/issues/1488#issuecomment-566278703
     * org.springframework.integration.json.ObjectToJsonTransformer#jsonObjectMapper
     * @return
     */
    @Bean
    public Jackson2JsonObjectMapper jacksonJsonBuilder() {
        Jackson2JsonObjectMapper b = new Jackson2JsonObjectMapper();
        ObjectMapper mapper = b.getObjectMapper();
        Map<Class<?>, Class<?>> mixIns = new LinkedHashMap<>();
        mixIns.put(org.springframework.batch.core.StepExecution.class, StepExecutionsMixin.class);
        mixIns.put(org.springframework.batch.core.JobExecution.class, JobExecutionMixin.class);
        mixIns.forEach(mapper::addMixIn);
        return b;
    }

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private RemotePartitioningManagerStepBuilderFactory partitionStepBuilderFactory;

    @Autowired
    private RemoteChunkingManagerStepBuilderFactory chunkingStepBuilderFactory;

    @Bean
    public DirectChannel requests() {
        return new DirectChannel();
    }

    @Bean
    public QueueChannel replies() {
        return new QueueChannel();
    }

    @Bean(name = "outboundFlow")
    public IntegrationFlow outboundFlow(@Qualifier("amazonSQSRequestAsync") AmazonSQSAsync sqsAsync) {
        SqsMessageHandler sqsMessageHandler = new SqsMessageHandler(sqsAsync);
        sqsMessageHandler.setQueue(appConfiguration.getRequestQueName());
        return IntegrationFlows.from(requests()).transform(objectToJsonTransformer()).log().handle(sqsMessageHandler).get();
    }

    @Bean(name = "inboundFlow")
    public IntegrationFlow inboundFlow(@Qualifier("amazonSQSReplyAsync") AmazonSQSAsync sqsAsync) {
        SqsMessageHandler sqsMessageHandler = new SqsMessageHandler(sqsAsync);
        sqsMessageHandler.setQueue(appConfiguration.getRequestQueName());
        return IntegrationFlows.from(replies()).transform(objectToJsonTransformer()).log().handle(sqsMessageHandler).get();
    }

    @Bean
    public ObjectToJsonTransformer objectToJsonTransformer() {
        return new ObjectToJsonTransformer(jacksonJsonBuilder());
    }

    // read - process and write will be made in slave
    public Job remotePartitionJob(User user) {
        // TODO: remove - date and   check if job is failed and restart
        return jobBuilderFactory.get(String.format("%s-%s-%s", "partitioningJob", user.getId(), new Date().getTime()))
            .start(partitionerStep(user))
            .incrementer(new RunIdIncrementer())
            .build();
    }

    public Job simpleChunkJob(User user) {
        return jobBuilderFactory.get(String.format("%s-%s-%s", "simpleChunkJob", user.getId(), new Date().getTime()))
            .start(chunkStep(user))
            .incrementer(new RunIdIncrementer())
            .build();
    }

//    @SneakyThrows
    public FilesPartitioner partitioner(User user) {
        return new FilesPartitioner(user);
    }

    public Step partitionerStep(User user) {
        return partitionStepBuilderFactory.get("partitionerStep")
                .partitioner(appConfiguration.getStepName(), partitioner(user))
                .outputChannel(requests())
//                .inputChannel(replies())
                .build();
    }

    public <T> FilesChunkReader<FileMeta> filesReader(User user) {
        return new FilesChunkReader(user.getId());
    }

    public Step chunkStep(User user) {
        return chunkingStepBuilderFactory.get("partitionerStep")
            .<File, File>chunk(5)
            .reader(filesReader(user))
            .outputChannel(requests())
            .inputChannel(replies())
            .build();
    }

    public void scheduleJobForUser(User user) {
        CompletableFuture completableFuture = new CompletableFuture();
        synchronized (user.getId()) {
            completableFuture.completeAsync(() -> {
                try {
                    JobParameters params = new JobParametersBuilder().addString(Constants.USER_ID, user.getId()).toJobParameters();
                    Job userJob = remotePartitionJob(user);
//                    Job userJob = simpleChunkJob(user);
                    jobLauncher.run(userJob, params);
                } catch (JobExecutionAlreadyRunningException ex) {
                    logger.info(String.format("job already scheduled for %s", user.getId()), ex);
                } catch (JobInstanceAlreadyCompleteException e) {
                    logger.info(String.format("job already completed for %s", user.getId()), e);
                } catch (JobParametersInvalidException e) {
                    logger.error(String.format("job parameters are invalid for %s", user.getId()), e);
                    throw new RuntimeException(e);
                } catch (JobRestartException e) {
                    logger.error(String.format("restart job to continue for %s", user.getId()), e);
                    throw new RuntimeException(e);
                }
                return null;
            });
        }
    }
}
