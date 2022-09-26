package com.oslash.integration.manager.config;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.oslash.integration.config.AppConfiguration;
import com.oslash.integration.manager.reader.FilesPartitioner;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.aws.outbound.SqsMessageHandler;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;

import java.util.Date;
import java.util.concurrent.CompletableFuture;

/**
 * The type Manager configuration.
 */
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
    private RemotePartitioningManagerStepBuilderFactory partitionStepBuilderFactory;
    @Value("${app.batch.manager.partition-size}")
    private Integer managerPartitionSize;

    /**
     * Requests direct channel.
     *
     * @return the direct channel
     */
    @Bean
    public DirectChannel requests() {
        return new DirectChannel();
    }

    /**
     * Replies queue channel.
     *
     * @return the queue channel
     */
    @Bean
    public QueueChannel replies() {
        return new QueueChannel();
    }

    /**
     * Outbound flow integration flow.
     *
     * @param sqsAsync the sqs async
     * @return the integration flow
     */
    @Bean(name = "outboundFlow")
    public IntegrationFlow outboundFlow(@Qualifier("amazonSQSRequestAsync") AmazonSQSAsync sqsAsync) {
        SqsMessageHandler sqsMessageHandler = new SqsMessageHandler(sqsAsync);
        sqsMessageHandler.setQueue(appConfiguration.getRequestQueName());
        return IntegrationFlows.from(requests()).transform(appConfiguration.objectToJsonTransformer()).log().handle(sqsMessageHandler).get();
    }

    /**
     * Inbound flow integration flow.
     *
     * @param sqsAsync the sqs async
     * @return the integration flow
     */
    @Bean(name = "inboundFlow")
    public IntegrationFlow inboundFlow(@Qualifier("amazonSQSReplyAsync") AmazonSQSAsync sqsAsync) {
        SqsMessageHandler sqsMessageHandler = new SqsMessageHandler(sqsAsync);
        sqsMessageHandler.setQueue(appConfiguration.getReplyQueName());
        return IntegrationFlows.from(replies()).transform(appConfiguration.objectToJsonTransformer()).log().handle(sqsMessageHandler).get();
    }


    /**
     * Remote partition job job.
     *
     * @param user the user
     * @return the job
     */
// read - process and write will be made in slave
    public Job remotePartitionJob(User user) {
        return jobBuilderFactory.get(String.format("%s-%s-%s", "file-meta-job", user.getId(), new Date().getTime())).start(partitionerStep(user)).incrementer(new RunIdIncrementer()).build();
    }

    /**
     * Partitioner files partitioner.
     *
     * @param user the user
     * @return the files partitioner
     */
    public FilesPartitioner partitioner(User user) {
        return new FilesPartitioner(user, appConfiguration);
    }

    /**
     * Partitioner step step.
     *
     * @param user the user
     * @return the step
     */
    public Step partitionerStep(User user) {
        // move grid size to config
        return partitionStepBuilderFactory.get("partitionerStep").partitioner(Constants.WORKER_STEP_NAME, partitioner(user)).gridSize(managerPartitionSize).outputChannel(requests()).build();
    }

    /**
     * Schedule job for user.
     *
     * @param user the user
     */
    public void scheduleJobForUser(User user) {
        CompletableFuture completableFuture = new CompletableFuture();
        synchronized (user.getId()) {
            completableFuture.completeAsync(() -> {
                try {
                    JobParameters params = new JobParametersBuilder().addString(Constants.USER_ID, user.getId()).toJobParameters();
                    Job userJob = remotePartitionJob(user);
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
