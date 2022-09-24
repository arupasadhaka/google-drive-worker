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

@Configuration
public class MessageQueConfiguration {

    @Autowired
    AppConfiguration appConfiguration;

    @Value("${aws.sqs.endpoint}")
    String endPoint;

    @Value("${aws.region}")
    String awsRegion;

    @Value("${aws.accessKey}")
    String accessKey;

    @Value("${aws.secretKey}")
    String secretKey;

    @Bean(name="amazonSQSRequestAsync")
    public AmazonSQSAsync amazonSQSRequestAsync() {
        final AwsClientBuilder.EndpointConfiguration endpointConfiguration =
            new AwsClientBuilder.EndpointConfiguration(endPoint, awsRegion);
        final AmazonSQSAsync sqsAsync = AmazonSQSAsyncClientBuilder
            .standard()
            .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)))
            .withEndpointConfiguration(endpointConfiguration)
            .build();
        final ListQueuesResult listQueuesResult = sqsAsync.listQueues(appConfiguration.getRequestQueName());
        if (listQueuesResult.getQueueUrls().isEmpty()) {
            sqsAsync.createQueueAsync(appConfiguration.getRequestQueName());
        }
        return sqsAsync;
    }

    @Bean(name="amazonSQSReplyAsync")
    public AmazonSQSAsync amazonSQSReplyAsync() {
        final AwsClientBuilder.EndpointConfiguration endpointConfiguration =
                new AwsClientBuilder.EndpointConfiguration(endPoint, awsRegion);
        final AmazonSQSAsync sqsAsync = AmazonSQSAsyncClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)))
                .withEndpointConfiguration(endpointConfiguration)
                .build();
        final ListQueuesResult listQueuesResult = sqsAsync.listQueues(appConfiguration.getReplyQueName());
        if (listQueuesResult.getQueueUrls().isEmpty()) {
            sqsAsync.createQueueAsync(appConfiguration.getReplyQueName());
        }
        return sqsAsync;
    }
}
