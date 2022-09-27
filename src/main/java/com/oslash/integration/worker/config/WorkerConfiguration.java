package com.oslash.integration.worker.config;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.oslash.integration.config.AppConfiguration;
import com.oslash.integration.models.FileMeta;
import com.oslash.integration.service.FileStorageService;
import com.oslash.integration.utils.Constants;
import com.oslash.integration.worker.model.FileStorageInfo;
import com.oslash.integration.worker.transformer.MessageTransformer;
import com.oslash.integration.worker.writer.FileMetaWriter;
import com.oslash.integration.worker.writer.FileStorageWriter;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.batch.integration.partition.RemotePartitioningWorkerStepBuilderFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.oslash.integration.utils.Constants.*;

/**
 * The type Worker configuration.
 */
@Configuration
@Profile("worker")
public class WorkerConfiguration {
    /**
     * The Logger.
     */
    private static Logger logger = LoggerFactory.getLogger(WorkerConfiguration.class);

    /**
     * The App configuration.
     */
    @Autowired
    private AppConfiguration appConfiguration;

    /**
     * The File meta writer.
     */
    @Autowired
    private FileMetaWriter fileMetaWriter;

    /**
     * The File storage writer.
     */
    @Autowired
    private FileStorageWriter fileStorageWriter;

    @Value("${app.batch.worker.download.file.chunk-size}")
    private Integer downloadFileStepChunkSize;

    @Value("${app.batch.worker.download.meta.chunk-size}")
    private Integer downloadFileMetaStepChunkSize;

    /**
     * The Job repository.
     */
    @Autowired
    private JobRepository jobRepository;
    /**
     * The Transaction manager.
     */
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private RemotePartitioningWorkerStepBuilderFactory stepBuilderFactory;

    @Autowired
    private FileStorageService fileStorageService;


    /**
     * Inbound flow integration flow.
     *
     * @param sqsAsync the sqs async
     * @return the integration flow
     */
    @Bean
    @ConditionalOnMissingBean(value = ObjectNamingStrategy.class, search = SearchStrategy.CURRENT)
    public IntegrationFlow inboundFlow(@Qualifier("amazonSQSRequestAsync") AmazonSQSAsync sqsAsync) {
        SqsMessageDrivenChannelAdapter adapter = new SqsMessageDrivenChannelAdapter(sqsAsync, appConfiguration.getRequestQueName());
        return IntegrationFlows.from(adapter).transform(messageTransformer()).channel(requests()).get();
    }

    /**
     * Outbound flow integration flow.
     *
     * @param sqsAsync the sqs async
     * @return the integration flow
     */
    @Bean
    @ConditionalOnMissingBean(value = ObjectNamingStrategy.class, search = SearchStrategy.CURRENT)
    public IntegrationFlow outboundFlow(@Qualifier("amazonSQSReplyAsync") AmazonSQSAsync sqsAsync) {
        SqsMessageHandler sqsMessageHandler = new SqsMessageHandler(sqsAsync);
        sqsMessageHandler.setQueue(appConfiguration.getReplyQueName());
        return IntegrationFlows.from(replies()).transform(appConfiguration.objectToJsonTransformer()).log().handle(sqsMessageHandler).get();
    }

    /**
     * Message transformer transformer.
     *
     * @return the transformer
     */
    @Bean
    public Transformer messageTransformer() {
        return new MessageTransformer();
    }

    /**
     * Requests direct channel.
     *
     * @return the direct channel
     */
    @Bean
    public DirectChannel requests() {
        return new DirectChannel();
    }

    /**
     * Replies direct channel.
     *
     * @return the direct channel
     */
    @Bean
    public DirectChannel replies() {
        return new DirectChannel();
    }

    /**
     * File meta flow flow.
     *
     * @return the flow
     */
    public Flow fileMetaFlow() {
        Flow fileDownloadFlow = new FlowBuilder<Flow>("fileDownload-Flow")
                .start(fileDownloadStep())
                .build();

        Flow parallelFlow = new FlowBuilder<Flow>("fileMetaSave-Flow")
                .start(fileMetaSaveStep())
                .split(new SimpleAsyncTaskExecutor())
                .add(fileDownloadFlow)
                .build();

        return new FlowBuilder<Flow>("split-file-downloader-flow")
                .start(parallelFlow)
                .end();
    }

    /**
     * worker step bean name should be mapped with manager step while creating
     *
     * @return step step
     * @See com.oslash.integration.utils.Constants#WORKER_STEP_NAME
     */
    @Bean(name = Constants.WORKER_STEP_NAME)
    public Step workerStep() {
        return stepBuilderFactory.get(WORKER_STEP_NAME)
                .inputChannel(requests())
                .outputChannel(replies())
                .flow(fileMetaFlow())
                .build();
    }

    /**
     * File download step step.
     *
     * @return the step
     */
    @SuppressFBWarnings("NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS")
    public Step fileDownloadStep() {
        SimpleStepBuilder simpleStepBuilder = new SimpleStepBuilder(new StepBuilder(WORKER_FILE_DOWNLOADER_STEP_NAME));
        simpleStepBuilder.chunk(downloadFileStepChunkSize)
                .reader(fileStorageReader(null))
                .processor(fileStorageProcessor())
                .writer(fileStorageWriter());
        simpleStepBuilder.repository(jobRepository);
        simpleStepBuilder.transactionManager(transactionManager);
        return simpleStepBuilder.build();
    }

    /**
     * File meta save step.
     *
     * @return the step
     */
    @SuppressFBWarnings("NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS")
    public Step fileMetaSaveStep() {
        SimpleStepBuilder simpleStepBuilder = new SimpleStepBuilder(new StepBuilder(WORKER_FILE_META_STEP_NAME));
        simpleStepBuilder.chunk(downloadFileMetaStepChunkSize).reader(fileMetaReader(null)).processor(fileMetaProcessor()).writer(fileMetaWriter());
        simpleStepBuilder.repository(jobRepository);
        simpleStepBuilder.transactionManager(transactionManager);
        return simpleStepBuilder.build();
    }

    /**
     * Task executor simple async task executor.
     *
     * @return the simple async task executor
     */
    @Bean
    public SimpleAsyncTaskExecutor taskExecutor() {
        return new SimpleAsyncTaskExecutor();
    }

    /**
     * File meta writer async item writer.
     *
     * @return the async item writer
     */
    public AsyncItemWriter<FileMeta> fileMetaWriter() {
        AsyncItemWriter<FileMeta> writer = new AsyncItemWriter<>();
        writer.setDelegate(fileMetaWriter);
        return writer;
    }

    /**
     * File storage writer async item writer.
     *
     * @return the async item writer
     */
    public AsyncItemWriter<FileStorageInfo> fileStorageWriter() {
        AsyncItemWriter<FileStorageInfo> writer = new AsyncItemWriter<>();
        writer.setDelegate(fileStorageWriter);
        return writer;
    }

    /**
     * File storage processor async item processor.
     *
     * @return the async item processor
     */
    public AsyncItemProcessor<Map, FileStorageInfo> fileStorageProcessor() {
        AsyncItemProcessor<Map, FileStorageInfo> processor = new AsyncItemProcessor<>();
        processor.setDelegate(new ItemProcessor<>() {
            @SneakyThrows
            @Override
            public FileStorageInfo process(Map item) {
                return fileStorageService.getFileStorageInfo(item);
            }
        });
        processor.setTaskExecutor(new SimpleAsyncTaskExecutor());
        return processor;
    }

    /**
     * File meta processor async item processor.
     *
     * @return the async item processor
     */
    public AsyncItemProcessor<Map, FileMeta> fileMetaProcessor() {
        AsyncItemProcessor<Map, FileMeta> processor = new AsyncItemProcessor<>();
        processor.setDelegate(new ItemProcessor<>() {
            @Override
            public FileMeta process(Map item) {
                FileMeta fileMeta = new FileMeta.Builder().file(item).build();
                logger.info("Transforming file meta for file " + fileMeta.getId());
                return fileMeta;
            }
        });
        processor.setTaskExecutor(new SimpleAsyncTaskExecutor());
        return processor;
    }


    /**
     * File meta reader item reader.
     *
     * @param data the data
     * @return the item reader
     */
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

    /**
     * File storage reader item reader.
     *
     * @param data the data
     * @return the item reader
     */
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
