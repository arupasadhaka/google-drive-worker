package com.oslash.integration.manager.transformer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.integration.chunk.ChunkResponse;
import org.springframework.integration.json.JsonToObjectTransformer;
import org.springframework.integration.support.json.JsonObjectMapper;
import org.springframework.messaging.Message;

import java.util.Map;

public class ResponseTransformer extends JsonToObjectTransformer {

    public ResponseTransformer(JsonObjectMapper<?, ?> jsonObjectMapper) {
        super(jsonObjectMapper);
    }

    @Override
    protected Object doTransform(Message<?> message) {
        return buildChunkResponse(message);
    }

    @SneakyThrows
    private ChunkResponse buildChunkResponse(Message<?> message) {
        Map map = new ObjectMapper().readValue(message.getPayload().toString(), Map.class);
        Integer jobId = (Integer) map.get("jobId");
        Integer sequence = (Integer) map.get("sequence");
        String messageContent = (String) map.get("message");
        Boolean status = (Boolean) map.get("successful");
        StepContribution stepContribution = new StepContribution(new StepExecution("-", null));
        return new ChunkResponse(status, sequence, Long.valueOf(jobId), stepContribution, messageContent);
    }
}
