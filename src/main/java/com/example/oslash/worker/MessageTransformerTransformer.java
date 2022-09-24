package com.example.oslash.worker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.batch.integration.partition.StepExecutionRequest;
import org.springframework.integration.transformer.AbstractTransformer;
import org.springframework.messaging.Message;

import java.util.Map;

public class MessageTransformerTransformer extends AbstractTransformer {
    @Override
    protected Object doTransform(Message<?> message) {
        Map map = null;
        try {
            map = new ObjectMapper().readValue(message.getPayload().toString(), Map.class);
            StepExecutionRequest stepExecutionRequest = new StepExecutionRequest((String) map.get("stepName"), Long.valueOf((Integer) map.get("jobExecutionId")),
                    Long.valueOf((Integer) map.get("stepExecutionId")));
            return this.getMessageBuilderFactory().withPayload(stepExecutionRequest).build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
