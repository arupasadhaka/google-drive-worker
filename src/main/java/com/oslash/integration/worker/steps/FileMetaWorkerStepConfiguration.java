package com.oslash.integration.worker.steps;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oslash.integration.manager.job.ManagerConfiguration;
import com.oslash.integration.worker.transformer.RequestTransformer;
import com.oslash.integration.worker.transformer.ResponseTransformer;
import com.oslash.integration.worker.writer.FileMetaWriter;
import org.springframework.batch.core.step.item.SimpleChunkProcessor;
import org.springframework.batch.integration.chunk.ChunkProcessorChunkHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.integration.aws.inbound.SqsMessageDrivenChannelAdapter;
import org.springframework.integration.aws.outbound.SqsMessageHandler;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.support.json.Jackson2JsonObjectMapper;
import org.springframework.messaging.MessageChannel;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration()
@Profile("manager")
public class FileMetaWorkerStepConfiguration {

    private final String REQUEST_QUEUE_NAME = "step1-request.fifo";
    private final String RESPONSE_QUEUE_NAME = "step1-response.fifo";

    @Autowired
    FileMetaWriter fileMetaWriter;

    @Autowired
    AmazonSQSAsync amazonSQSAsync;

    public Jackson2ObjectMapperBuilder jacksonBuilder() {
        Jackson2ObjectMapperBuilder b = new Jackson2ObjectMapperBuilder();
        b.indentOutput(true).mixIn(org.springframework.batch.core.StepExecution.class, ManagerConfiguration.StepExecutionsMixin.class).mixIn(org.springframework.batch.core.JobExecution.class, ManagerConfiguration.JobExecutionMixin.class);
        return b;
    }

    /**
     * issue: https://github.com/spring-projects/spring-batch/issues/1488#issuecomment-566278703
     * org.springframework.integration.json.ObjectToJsonTransformer#jsonObjectMapper
     */
    public Jackson2JsonObjectMapper jacksonJsonBuilder() {
        Jackson2JsonObjectMapper b = new Jackson2JsonObjectMapper();
        ObjectMapper mapper = b.getObjectMapper();
        Map<Class<?>, Class<?>> mixIns = new LinkedHashMap<>();
        mixIns.put(org.springframework.batch.core.StepExecution.class, ManagerConfiguration.StepExecutionsMixin.class);
        mixIns.put(org.springframework.batch.core.JobExecution.class, ManagerConfiguration.JobExecutionMixin.class);
        mixIns.forEach(mapper::addMixIn);
        return b;
    }

    private RequestTransformer requestTransformer() {
        return new RequestTransformer(jacksonJsonBuilder());
    }

    private ResponseTransformer responseTransformer() {
        return new ResponseTransformer(jacksonJsonBuilder());
    }

    // Processor
    @Bean
    public ChunkProcessorChunkHandler workerChunkProcessorChunkHandler() {
        ChunkProcessorChunkHandler slaveChunkProcessorChunkHandler = new ChunkProcessorChunkHandler();
        slaveChunkProcessorChunkHandler.setChunkProcessor(new SimpleChunkProcessor(null, fileMetaWriter));
        return slaveChunkProcessorChunkHandler;
    }

    // Request
    @Bean
    public IntegrationFlow workerRequestIntegrationFlow() {
        return IntegrationFlows.from(buildRequestSqsMessageDrivenChannelAdapter())
                .transform(requestTransformer())
                .handle(workerChunkProcessorChunkHandler())
                .channel(workerResponseMessageChannel())
                .get();
    }

    @Bean
    public MessageChannel step1RequestMessageChannel() {
        return new DirectChannel();
    }

    private SqsMessageDrivenChannelAdapter buildRequestSqsMessageDrivenChannelAdapter() {
        SqsMessageDrivenChannelAdapter adapter = new SqsMessageDrivenChannelAdapter(amazonSQSAsync, REQUEST_QUEUE_NAME);
        adapter.setOutputChannel(workerResponseMessageChannel());
        // ACK in transformer
        adapter.setMessageDeletionPolicy(SqsMessageDeletionPolicy.NEVER);
        adapter.setMaxNumberOfMessages(1);
        return adapter;
    }

    // Response
    @Bean
    public IntegrationFlow workerResponseIntegrationFlow() {
        return IntegrationFlows.from(workerResponseMessageChannel()).
                transform(responseTransformer())
                .handle(sqsMessageHandler())
                .get();
    }

    @Bean
    public MessageChannel workerResponseMessageChannel() {
        return new DirectChannel();
    }

    private SqsMessageHandler sqsMessageHandler() {
        SqsMessageHandler sqsMessageHandler = new SqsMessageHandler(amazonSQSAsync);
        sqsMessageHandler.setQueue(RESPONSE_QUEUE_NAME);

        return sqsMessageHandler;
    }

}
