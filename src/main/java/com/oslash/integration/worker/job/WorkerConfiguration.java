package com.oslash.integration.worker.job;

import com.oslash.integration.config.AppConfiguration;
import org.springframework.batch.integration.partition.BeanFactoryStepLocator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("worker")
public class WorkerConfiguration {
    @Autowired
    AppConfiguration appConfiguration;

    @Bean
    public BeanFactoryStepLocator beanFactoryStepLocator() {
        return new BeanFactoryStepLocator();
    }
}
