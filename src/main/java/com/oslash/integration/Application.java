package com.oslash.integration;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.integration.config.annotation.EnableBatchIntegration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;

/**
 * The type Application.
 */
@EnableBatchProcessing
@EnableBatchIntegration
@SpringBootApplication(exclude = {JmxAutoConfiguration.class})
public class Application {
    /**
     * The entry point of application.
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
