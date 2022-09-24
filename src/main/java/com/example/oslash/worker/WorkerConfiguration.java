package com.example.oslash.worker;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.example.oslash.models.FileMeta;
import com.example.oslash.utils.Constants;
import com.example.oslash.worker.writer.FileMetaWriter;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.integration.partition.RemotePartitioningWorkerStepBuilderFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.aws.inbound.SqsMessageDrivenChannelAdapter;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.transformer.Transformer;
import org.springframework.jmx.export.naming.ObjectNamingStrategy;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

@Configuration
@Profile("worker")
public class WorkerConfiguration {
    @Autowired
    FileMetaWriter fileMetaWriter;
    @Autowired
    private JobBuilderFactory jobBuilderFactory;
    @Autowired
    private RemotePartitioningWorkerStepBuilderFactory stepBuilderFactory;

    @Autowired
    private DataSource dataSource;

    @Bean
    @ConditionalOnMissingBean(value = ObjectNamingStrategy.class, search = SearchStrategy.CURRENT)
    public IntegrationFlow inboundFlow(AmazonSQSAsync sqsAsync) {
        SqsMessageDrivenChannelAdapter adapter = new SqsMessageDrivenChannelAdapter(sqsAsync, Constants.QUEUE_NAME);
        return IntegrationFlows.from(adapter).transform(jsonToObjectTransformer()).channel(inboundRequests()).get();
    }

    @Bean
    public Transformer jsonToObjectTransformer() {
        return new MessageTransformerTransformer();
    }

    @Bean
    public QueueChannel inboundRequests() {
        return new QueueChannel();
    }

    @Bean(name = "simpleStep")
    public Step simpleStep() {
        return stepBuilderFactory.get(Constants.WORKER_STEP_NAME)
                .inputChannel(inboundRequests())
                .<Integer, FileMeta>chunk(100)
                .reader(itemReader(null))
                .processor(itemProcessor())
                .writer(itemWriter())
                .build();
    }

    @Bean
    public ItemWriter<FileMeta> itemWriter() {
        return fileMetaWriter;
    }

    @Bean
    public ItemProcessor<Integer, FileMeta> itemProcessor() {
        return new ItemProcessor<>() {
            @Override
            public FileMeta process(Integer item) {
                return new FileMeta.Builder().content(item).build();
            }
        };
    }

    @Bean
    @StepScope
    public ItemReader<Integer> itemReader(@Value("#{stepExecutionContext['data']}") List<Integer> data) {
        List<Integer> remainingData = new ArrayList<>(data);
        return new ItemReader<>() {
            @Override
            public Integer read() {
                if (remainingData.size() > 0) {
                    return remainingData.remove(0);
                }

                return null;
            }
        };
    }
}
