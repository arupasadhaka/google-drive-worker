package com.oslash.integration.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfiguration {

    @Value("${app.sqs.que.request-name}")
    private String requestQueName;

    @Value("${app.sqs.que.reply-name}")
    private String replyQueName;

    @Value("${app.batch.step.name}")
    private String stepName;

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
