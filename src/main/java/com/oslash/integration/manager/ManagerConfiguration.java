package com.oslash.integration.manager;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.oslash.integration.config.AppConfiguration;
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

@Configuration
@Profile("manager")
public class ManagerConfiguration {
    private final Logger logger = LoggerFactory.getLogger(ManagerConfiguration.class);

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


    public Job partitionerJob(User user) {
        return jobBuilderFactory.get("partitioningJob")
                .start(partitionerStep(user))
                .incrementer(new RunIdIncrementer())
                .build();
    }


    public FileMetaPartitioner partitioner(User user) {
        return new FileMetaPartitioner(user);
    }

    public Step partitionerStep(User user) {
        return stepBuilderFactory.get("partitionerStep")
                .partitioner(appConfiguration.getStepName(), partitioner(user))
                .outputChannel(outboundRequests())
                .build();
    }

    public void scheduleJobForUser(User user) {
        CompletableFuture completableFuture = new CompletableFuture();
        synchronized (user.getId()) {
            completableFuture.completeAsync(() -> {
                try {
                    JobParameters params = new JobParametersBuilder().addString(Constants.USER_ID, user.getId()).toJobParameters();
                    Job userJob = jobBuilderFactory.get(String.format("%s-%s", "partitioningJob", user.getId()))
                            .start(partitionerStep(user))
                            .incrementer(new RunIdIncrementer())
                            .build();
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
