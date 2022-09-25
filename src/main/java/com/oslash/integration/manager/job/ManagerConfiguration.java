package com.oslash.integration.manager.job;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oslash.integration.config.AppConfiguration;
import com.oslash.integration.manager.listener.ManagerExecutionListener;
import com.oslash.integration.manager.steps.FileMetaManagerStepConfiguration;
import com.oslash.integration.models.User;
import com.oslash.integration.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
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
    FileMetaManagerStepConfiguration stepConfiguration;

    @Autowired
    ManagerExecutionListener listener;

    public Job downloadFilesForUserJob(User user) {
        return jobBuilderFactory.get(String.format("%s-job-%s-%s", "chunking", user.getId(), new Date().getTime()))
                .listener(listener)
                .start(stepConfiguration.filesMetaManagerStep(user))
                .build();
    }

    public void scheduleJobForUser(User user) {
        CompletableFuture completableFuture = new CompletableFuture();
        synchronized (user.getId()) {
            completableFuture.completeAsync(() -> {
                try {
                    JobParameters params = new JobParametersBuilder().addString(Constants.USER_ID, user.getId()).toJobParameters();
                    Job userJob = downloadFilesForUserJob(user);
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
