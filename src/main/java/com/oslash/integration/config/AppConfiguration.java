package com.oslash.integration.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oslash.integration.manager.config.ManagerConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.json.ObjectToJsonTransformer;
import org.springframework.integration.support.json.Jackson2JsonObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class AppConfiguration {

    @Value("${app.sqs.que.request-name}")
    private String requestQueName;

    @Value("${app.sqs.que.reply-name}")
    private String replyQueName;

    @Value("${app.batch.step.name}")
    private String stepName;

    @Bean
    public ObjectToJsonTransformer objectToJsonTransformer() {
        return new ObjectToJsonTransformer(jacksonJsonBuilder());
    }

    /**
     * <p>
     * fix step execution serialise with circular dependency
     * </p>
     *
     * @See {@link <a href="https://github.com/spring-projects/spring-batch/issues/1488#issuecomment-566278703">issue</a>}
     * @See org.springframework.integration.json.ObjectToJsonTransformer#jsonObjectMapper
     */
    @Bean
    public Jackson2JsonObjectMapper jacksonJsonBuilder() {
        Jackson2JsonObjectMapper b = new Jackson2JsonObjectMapper();
        ObjectMapper mapper = b.getObjectMapper();
        Map<Class<?>, Class<?>> mixIns = new LinkedHashMap<>();
        mixIns.put(org.springframework.batch.core.StepExecution.class, ManagerConfiguration.StepExecutionsMixin.class);
        mixIns.put(org.springframework.batch.core.JobExecution.class, ManagerConfiguration.JobExecutionMixin.class);
        mixIns.forEach(mapper::addMixIn);
        return b;
    }

    public String getRequestQueName() {
        return requestQueName;
    }

    public void setRequestQueName(String requestQueName) {
        this.requestQueName = requestQueName;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public String getReplyQueName() {
        return replyQueName;
    }

    public void setReplyQueName(String replyQueName) {
        this.replyQueName = replyQueName;
    }
}
