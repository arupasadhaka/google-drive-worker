package com.oslash.integration.manager;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.google.api.services.people.v1.model.Person;
import com.oslash.integration.config.AppConfiguration;
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
import org.springframework.batch.integration.partition.RemotePartitioningManagerStepBuilderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.aws.outbound.SqsMessageHandler;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.json.ObjectToJsonTransformer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@Profile("manager")
public class ManagerConfiguration {
    private final Logger logger = LoggerFactory.getLogger(ManagerConfiguration.class);

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Autowired
    private AppConfiguration appConfiguration;


    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private RemotePartitioningManagerStepBuilderFactory stepBuilderFactory;

    @Bean
    public DirectChannel outboundRequests() {
        return new DirectChannel();
    }

    @Bean
    public IntegrationFlow outboundFlow(AmazonSQSAsync sqsAsync) {
        SqsMessageHandler sqsMessageHandler = new SqsMessageHandler(sqsAsync);
        sqsMessageHandler.setQueue(appConfiguration.getQueName());
        return IntegrationFlows.from(outboundRequests()).transform(objectToJsonTransformer()).log().handle(sqsMessageHandler).get();
    }

    @Bean
    public ObjectToJsonTransformer objectToJsonTransformer() {
        return new ObjectToJsonTransformer();
    }

    @Bean
    public Job partitionerJob() {
        return jobBuilderFactory.get("partitioningJob").start(partitionerStep()).incrementer(new RunIdIncrementer()).build();
    }

    @Bean
    public FileMetaPartitioner partitioner() {
        return new FileMetaPartitioner();
    }

    @Bean
    public Step partitionerStep() {
        return stepBuilderFactory.get("partitionerStep").partitioner(appConfiguration.getStepName(), partitioner()).outputChannel(outboundRequests()).build();
    }

    public void scheduleJobForUser(Person userDetails) {
        CompletableFuture completableFuture = new CompletableFuture();
        synchronized (userDetails.getResourceName()) {
            completableFuture.completeAsync(() -> {
                try {
                    JobParameters params = new JobParametersBuilder().addString(Constants.USER_ID, userDetails.getResourceName()).toJobParameters();
                    Job userJob = jobBuilderFactory.get(String.format("%s-%s", "partitioningJob", userDetails.getResourceName())).start(partitionerStep()).incrementer(new RunIdIncrementer()).preventRestart().build();
                    jobLauncher.run(userJob, params);
                } catch (JobExecutionAlreadyRunningException ex) {
                    logger.info(String.format("job already scheduled for %s", userDetails.getResourceName()), ex);
                } catch (JobInstanceAlreadyCompleteException e) {
                    logger.info(String.format("job already completed for %s", userDetails.getResourceName()), e);
                } catch (JobParametersInvalidException e) {
                    logger.error(String.format("job parameters are invalid for %s", userDetails.getResourceName()), e);
                    throw new RuntimeException(e);
                } catch (JobRestartException e) {
                    logger.error(String.format("restart job to continue for %s", userDetails.getResourceName()), e);
                    throw new RuntimeException(e);
                }
                return null;
            });
        }
    }
}
