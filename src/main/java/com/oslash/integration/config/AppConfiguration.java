package com.oslash.integration.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.integration.json.ObjectToJsonTransformer;
import org.springframework.integration.support.json.Jackson2JsonObjectMapper;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The type App configuration.
 */
@Configuration
public class AppConfiguration {

    @Value("${app.sqs.que.request-name}")
    private String requestQueName;

    // can be used in remote chunking
    @Value("${app.sqs.que.reply-name}")
    private String replyQueName;

    @Value("${app.files.mime-type}")
    private String mimeType;

    /**
     * Object to json transformer object to json transformer.
     *
     * @return the object to json transformer
     */
    @Bean
    public ObjectToJsonTransformer objectToJsonTransformer() {
        return new ObjectToJsonTransformer(jacksonJsonBuilder());
    }

    /**
     * Jackson builder jackson 2 object mapper builder.
     *
     * @return the jackson 2 object mapper builder
     */
    @Bean
    public Jackson2ObjectMapperBuilder jacksonBuilder() {
        Jackson2ObjectMapperBuilder b = new Jackson2ObjectMapperBuilder();
        b.indentOutput(true)
                .mixIn(org.springframework.batch.core.StepExecution.class, StepExecutionsMixin.class)
                .mixIn(org.springframework.batch.core.JobExecution.class, JobExecutionMixin.class);
        return b;
    }

    /**
     * <p>
     * fix step execution serialise with circular dependency
     * </p>
     *
     * @return the jackson 2 json object mapper
     * @See {@link <a href="https://github.com/spring-projects/spring-batch/issues/1488#issuecomment-566278703">issue</a>}
     * @See org.springframework.integration.json.ObjectToJsonTransformer#jsonObjectMapper
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

    /**
     * Gets request que name.
     *
     * @return the request que name
     */
    public String getRequestQueName() {
        return requestQueName;
    }

    /**
     * Sets request que name.
     *
     * @param requestQueName the request que name
     */
    public void setRequestQueName(String requestQueName) {
        this.requestQueName = requestQueName;
    }

    /**
     * Gets reply que name.
     *
     * @return the reply que name
     */
    public String getReplyQueName() {
        return replyQueName;
    }

    /**
     * Sets reply que name.
     *
     * @param replyQueName the reply que name
     */
    public void setReplyQueName(String replyQueName) {
        this.replyQueName = replyQueName;
    }

    /**
     * Gets mime type.
     *
     * @return the mime type
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Sets mime type.
     *
     * @param mimeType the mime type
     */
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * The type Job execution mixin.
     */
    public abstract static class JobExecutionMixin {
        @JsonManagedReference
        private Collection<StepExecution> stepExecutions;
    }

    /**
     * The type Step executions mixin.
     */
    public abstract static class StepExecutionsMixin {
        @JsonIgnore
        private JobExecution jobExecution;
    }
}
