package com.oslash.integration.manager.steps;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oslash.integration.manager.job.ManagerConfiguration;
import com.oslash.integration.manager.transformer.RequestTransformer;
import com.oslash.integration.manager.transformer.ResponseTransformer;
import com.oslash.integration.manager.steps.reader.FileMetaReader;
import com.oslash.integration.models.User;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.integration.chunk.ChunkMessageChannelItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.integration.aws.inbound.SqsMessageDrivenChannelAdapter;
import org.springframework.integration.aws.outbound.SqsMessageHandler;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.json.ObjectToJsonTransformer;
import org.springframework.integration.support.json.Jackson2JsonObjectMapper;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.PollableChannel;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@Profile("manager")
public class FileMetaManagerStepConfiguration {

    private final String REQUEST_QUEUE_NAME = "step1-request.fifo";
    private final String RESPONSE_QUEUE_NAME = "step1-response.fifo";

    @Autowired
    StepBuilderFactory stepBuilderFactory;

    @Autowired
    AmazonSQSAsync amazonSQSAsync;

    @Bean
    public Jackson2ObjectMapperBuilder jacksonBuilder() {
        Jackson2ObjectMapperBuilder b = new Jackson2ObjectMapperBuilder();
        b.indentOutput(true).mixIn(org.springframework.batch.core.StepExecution.class, ManagerConfiguration.StepExecutionsMixin.class).mixIn(org.springframework.batch.core.JobExecution.class, ManagerConfiguration.JobExecutionMixin.class);
        return b;
    }

    /**
     * issue: https://github.com/spring-projects/spring-batch/issues/1488#issuecomment-566278703
     * org.springframework.integration.json.ObjectToJsonTransformer#jsonObjectMapper
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

    @Bean
    public ObjectToJsonTransformer objectToJsonTransformer() {
        return new ObjectToJsonTransformer(jacksonJsonBuilder());
    }

    private RequestTransformer requestTransformer() {
        return new RequestTransformer(jacksonJsonBuilder());
    }

    private ResponseTransformer responseTransformer() {
        return new ResponseTransformer(jacksonJsonBuilder());
    }

    public Step filesMetaManagerStep(User user) {
        Integer chunkSize = 10;
        return stepBuilderFactory.get("filesMetaManagerStep")
            .chunk(chunkSize)
            .reader(new FileMetaReader(user.getId()))
            .writer(chunkMessageChannelItemWriter(chunkSize))
            .build();
    }

    private ChunkMessageChannelItemWriter chunkMessageChannelItemWriter(Integer chunkSize) {
        ChunkMessageChannelItemWriter chunkMessageChannelItemWriter = new ChunkMessageChannelItemWriter();
        chunkMessageChannelItemWriter.setMessagingOperations(buildMessagingTemplate(managerRequestMesssageChannel()));
        chunkMessageChannelItemWriter.setReplyChannel(managerResponseMessageChannel());
        chunkMessageChannelItemWriter.setThrottleLimit(chunkSize);
        return chunkMessageChannelItemWriter;
    }

    private MessagingTemplate buildMessagingTemplate(MessageChannel messageChannel) {
        MessagingTemplate messagingTemplate = new MessagingTemplate();
        messagingTemplate.setDefaultChannel(messageChannel);
        return messagingTemplate;
    }

    private MessageHandler sqsMessageHandler() {
        SqsMessageHandler sqsMessageHandler = new SqsMessageHandler(amazonSQSAsync);
        sqsMessageHandler.setQueue(REQUEST_QUEUE_NAME);
        return sqsMessageHandler;
    }

    // Request configuration
    @Bean
    public MessageChannel managerRequestMesssageChannel() {
        return new DirectChannel();
    }

    @Bean
    public IntegrationFlow managerRequestIntegrationFlow() {
        return IntegrationFlows.from(managerRequestMesssageChannel()).transform(requestTransformer()).handle(sqsMessageHandler()).get();
    }

    // Response configuration
    @Bean
    public PollableChannel managerResponseMessageChannel() {
        return new QueueChannel();
    }

    @Bean
    public IntegrationFlow managerResponseIntegrationFlow() {
        return IntegrationFlows.from(sqsMessageDrivenChannelAdapter()).transform(responseTransformer()).channel(managerResponseMessageChannel()).get();
    }

    private SqsMessageDrivenChannelAdapter sqsMessageDrivenChannelAdapter() {
        return new SqsMessageDrivenChannelAdapter(amazonSQSAsync, RESPONSE_QUEUE_NAME);
    }
}
