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

@Configuration
public class AppConfiguration {

    @Value("${app.sqs.que.request-name}")
    private String requestQueName;

    // can be used in remote chunking
    @Value("${app.sqs.que.reply-name}")
    private String replyQueName;

    @Value("${app.files.mime-type}")
    private String mimeType;

    @Bean
    public ObjectToJsonTransformer objectToJsonTransformer() {
        return new ObjectToJsonTransformer(jacksonJsonBuilder());
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
        mixIns.put(org.springframework.batch.core.StepExecution.class, StepExecutionsMixin.class);
        mixIns.put(org.springframework.batch.core.JobExecution.class, JobExecutionMixin.class);
        mixIns.forEach(mapper::addMixIn);
        return b;
    }

    public String getRequestQueName() {
        return requestQueName;
    }

    public void setRequestQueName(String requestQueName) {
        this.requestQueName = requestQueName;
    }

    public String getReplyQueName() {
        return replyQueName;
    }

    public void setReplyQueName(String replyQueName) {
        this.replyQueName = replyQueName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public abstract class JobExecutionMixin {
        @JsonManagedReference
        private Collection<StepExecution> stepExecutions;
    }

    public abstract class StepExecutionsMixin {
        @JsonIgnore
        private JobExecution jobExecution;
    }
}
