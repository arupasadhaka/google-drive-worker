package com.oslash.integration.manager.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class ManagerExecutionListener implements JobExecutionListener {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void beforeJob(JobExecution jobExecution) {}

    @Override
    public void afterJob(JobExecution jobExecution) {
        logger.info("Job finished in " + diffTimeInSeconds(jobExecution.getStartTime(), jobExecution.getEndTime()) + " seconds");
    }

    private Long diffTimeInSeconds(Date startTime, Date endTime) {
        return startTime != null && endTime != null ? (endTime.getTime() - startTime.getTime()) / 1000 : null;
    }
}
