package com.oslash.integration.config.sqs;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.amazonaws.services.sqs.model.ListQueuesResult;
import com.oslash.integration.config.AppConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The type Message que configuration.
 */
@Configuration
public class MessageQueConfiguration {

    /**
     * The App configuration.
     */
    @Autowired
    AppConfiguration appConfiguration;

    /**
     * The End point.
     */
    @Value("${aws.sqs.endpoint}")
    String endPoint;

    /**
     * The Aws region.
     */
    @Value("${aws.region}")
    String awsRegion;

    /**
     * The Access key.
     */
    @Value("${aws.accessKey}")
    String accessKey;

    /**
     * The Secret key.
     */
    @Value("${aws.secretKey}")
    String secretKey;

    /**
     * Amazon sqs request async amazon sqs async.
     *
     * @return the amazon sqs async
     */
    @Bean(name = "amazonSQSRequestAsync")
    public AmazonSQSAsync amazonSQSRequestAsync() {
        final AwsClientBuilder.EndpointConfiguration endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(endPoint, awsRegion);
        final AmazonSQSAsync sqsAsync = AmazonSQSAsyncClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey))).withEndpointConfiguration(endpointConfiguration).build();
        final ListQueuesResult listQueuesResult = sqsAsync.listQueues(appConfiguration.getRequestQueName());
        if (listQueuesResult.getQueueUrls().isEmpty()) {
            sqsAsync.createQueueAsync(appConfiguration.getRequestQueName());
        }
        return sqsAsync;
    }

    /**
     * Amazon sqs reply async amazon sqs async.
     *
     * @return the amazon sqs async
     */
    @Bean(name = "amazonSQSReplyAsync")
    public AmazonSQSAsync amazonSQSReplyAsync() {
        final AwsClientBuilder.EndpointConfiguration endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(endPoint, awsRegion);
        final AmazonSQSAsync sqsAsync = AmazonSQSAsyncClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey))).withEndpointConfiguration(endpointConfiguration).build();
        final ListQueuesResult listQueuesResult = sqsAsync.listQueues(appConfiguration.getReplyQueName());
        if (listQueuesResult.getQueueUrls().isEmpty()) {
            sqsAsync.createQueueAsync(appConfiguration.getReplyQueName());
        }
        return sqsAsync;
    }
}
