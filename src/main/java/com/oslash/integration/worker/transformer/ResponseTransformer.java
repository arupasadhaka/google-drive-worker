package com.oslash.integration.worker.transformer;

import org.springframework.integration.json.JsonToObjectTransformer;
import org.springframework.integration.support.json.JsonObjectMapper;
import org.springframework.messaging.Message;

public class ResponseTransformer extends JsonToObjectTransformer {

    private static final String MESSAGE_GROUP_ID_HEADER = "message-group-id";

    public ResponseTransformer(JsonObjectMapper<?, ?> jsonObjectMapper) {
        super(jsonObjectMapper);
    }

    @Override
    protected Object doTransform(Message<?> message) {
        Message jsonMessage = (Message) super.doTransform(message);

        return this.getMessageBuilderFactory().withPayload(jsonMessage.getPayload()).setHeader(MESSAGE_GROUP_ID_HEADER, "unique").build();
    }
}
