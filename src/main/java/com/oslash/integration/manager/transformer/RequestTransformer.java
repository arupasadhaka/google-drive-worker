package com.oslash.integration.manager.transformer;

import org.springframework.integration.json.ObjectToJsonTransformer;
import org.springframework.integration.support.json.JsonObjectMapper;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

public class RequestTransformer extends ObjectToJsonTransformer {

    public RequestTransformer() {
    }

    public RequestTransformer(JsonObjectMapper jsonObjectMapper) {
        super(jsonObjectMapper);
    }

    private static final String MESSAGE_GROUP_ID_HEADER = "message-group-id";

    @Override
    protected Object doTransform(Message<?> message) {
        Message jsonMessage = (Message) super.doTransform(message);

        return this.getMessageBuilderFactory().withPayload(jsonMessage.getPayload()).setHeader(MESSAGE_GROUP_ID_HEADER, "unique").build();
    }
}