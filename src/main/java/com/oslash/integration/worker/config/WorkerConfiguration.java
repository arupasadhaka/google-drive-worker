package com.oslash.integration.worker.config;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.google.api.services.drive.Drive;
import com.oslash.integration.config.AppConfiguration;
import com.oslash.integration.models.FileMeta;
import com.oslash.integration.models.FileStorage;
import com.oslash.integration.utils.Constants;
import com.oslash.integration.worker.model.FileStorageInfo;
import com.oslash.integration.worker.transformer.MessageTransformer;
import com.oslash.integration.worker.writer.FileMetaWriter;
import com.oslash.integration.worker.writer.FileStorageWriter;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.SimpleStepBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.integration.partition.RemotePartitioningWorkerStepBuilderFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.integration.aws.inbound.SqsMessageDrivenChannelAdapter;
import org.springframework.integration.aws.outbound.SqsMessageHandler;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.transformer.Transformer;
import org.springframework.jmx.export.naming.ObjectNamingStrategy;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.oslash.integration.resolver.IntegrationResolver.integrationResolver;
import static com.oslash.integration.utils.Constants.*;

@Configuration
@Profile("worker")
public class WorkerConfiguration {
    Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    AppConfiguration appConfiguration;

    @Autowired
    FileMetaWriter fileMetaWriter;

    @Autowired
    FileStorageWriter fileStorageWriter;

    @Autowired
    JobRepository jobRepository;
    @Autowired
    PlatformTransactionManager transactionManager;
    @Autowired
    private RemotePartitioningWorkerStepBuilderFactory stepBuilderFactory;

    @Bean
    @ConditionalOnMissingBean(value = ObjectNamingStrategy.class, search = SearchStrategy.CURRENT)
    public IntegrationFlow inboundFlow(@Qualifier("amazonSQSRequestAsync") AmazonSQSAsync sqsAsync) {
        SqsMessageDrivenChannelAdapter adapter = new SqsMessageDrivenChannelAdapter(sqsAsync, appConfiguration.getRequestQueName());
        return IntegrationFlows.from(adapter).transform(messageTransformer()).channel(requests()).get();
    }

    @Bean
    @ConditionalOnMissingBean(value = ObjectNamingStrategy.class, search = SearchStrategy.CURRENT)
    public IntegrationFlow outboundFlow(@Qualifier("amazonSQSReplyAsync") AmazonSQSAsync sqsAsync) {
        SqsMessageHandler sqsMessageHandler = new SqsMessageHandler(sqsAsync);
        sqsMessageHandler.setQueue(appConfiguration.getReplyQueName());
        return IntegrationFlows.from(replies()).transform(appConfiguration.objectToJsonTransformer()).log().handle(sqsMessageHandler).get();
    }

    @Bean
    public Transformer messageTransformer() {
        return new MessageTransformer();
    }

    @Bean
    public DirectChannel requests() {
        return new DirectChannel();
    }

    @Bean
    public DirectChannel replies() {
        return new DirectChannel();
    }

    public Flow fileMetaFlow() {
        // todo move this to async
        // .split(taskExecutor()).add(fileMetaSaveStep())
        return new FlowBuilder<Flow>("split-file-downloader-flow").from(fileMetaSaveStep()).next(fileDownloadStep()).build();
    }

    /**
     * worker step bean name should be mapped with manager step while creating
     *
     * @return
     * @See com.oslash.integration.utils.Constants#WORKER_STEP_NAME
     */
    @Bean(name = Constants.WORKER_STEP_NAME)
    public Step workerStep() {
        return stepBuilderFactory.get(WORKER_STEP_NAME).inputChannel(requests()).flow(fileMetaFlow()).build();
    }

    public Step fileDownloadStep() {
        SimpleStepBuilder simpleStepBuilder = new SimpleStepBuilder(new StepBuilder(WORKER_FILE_DOWNLOADER_STEP_NAME));
        // todo move chunk size to config
        simpleStepBuilder.<Map, FileMeta>chunk(5).reader(fileStorageReader(null)).processor(fileStorageProcessor()).writer(fileStorageWriter());
        simpleStepBuilder.repository(jobRepository);
        simpleStepBuilder.transactionManager(transactionManager);
        return simpleStepBuilder.build();
    }

    public Step fileMetaSaveStep() {
        SimpleStepBuilder simpleStepBuilder = new SimpleStepBuilder(new StepBuilder(WORKER_FILE_META_STEP_NAME));
        // todo move chunk size to config
        simpleStepBuilder.<Map, FileMeta>chunk(100).reader(fileMetaReader(null)).processor(fileMetaProcessor()).writer(fileMetaWriter());
        simpleStepBuilder.repository(jobRepository);
        simpleStepBuilder.transactionManager(transactionManager);
        return simpleStepBuilder.build();
    }

    @Bean
    public SimpleAsyncTaskExecutor taskExecutor() {
        return new SimpleAsyncTaskExecutor();
    }

    public ItemWriter<FileMeta> fileMetaWriter() {
        return fileMetaWriter;
    }

    public ItemWriter<FileStorageInfo> fileStorageWriter() {
        return fileStorageWriter;
    }

    public ItemProcessor<Map, FileStorageInfo> fileStorageProcessor() {
        return new ItemProcessor<>() {
            @SneakyThrows
            @Override
            public FileStorageInfo process(Map item) {
                final FileStorage fileStorage = new FileStorage.Builder().file(item).build();
                logger.info("Processing file downloader for file " + fileStorage.getFileId());
                final Drive drive = integrationResolver().resolveGDrive(fileStorage.getUserId());
                final InputStream fileStream = drive.files().export(fileStorage.getFileId(), Constants.MIME_TYPE_TEXT_PLAIN).executeMediaAsInputStream();
                return new FileStorageInfo.Builder().fileStream(fileStream).file(fileStorage).userId(fileStorage.getUserId()).build();
            }
        };
    }

    public ItemProcessor<Map, FileMeta> fileMetaProcessor() {
        return new ItemProcessor<>() {
            @Override
            public FileMeta process(Map item) {
                FileMeta fileMeta = new FileMeta.Builder().file(item).build();
                logger.info("Transforming file meta for file " + fileMeta.getId());
                return fileMeta;
            }
        };
    }


    @Bean
    @StepScope
    public ItemReader<Map> fileMetaReader(@Value("#{stepExecutionContext['data']}") List<Map> data) {
        List<Map> remainingData = new ArrayList<>(data);
        return new ItemReader<>() {
            @Override
            public Map read() {
                if (remainingData.size() > 0) {
                    return remainingData.remove(0);
                }
                return null;
            }
        };
    }

    @Bean
    @StepScope
    public ItemReader<Map> fileStorageReader(@Value("#{stepExecutionContext['data']}") List<Map> data) {
        List<Map> remainingData = new ArrayList<>(data);
        return new ItemReader<>() {
            @Override
            public Map read() {
                if (remainingData.size() > 0) {
                    return remainingData.remove(0);
                }
                return null;
            }
        };
    }
}
