package com.oslash.integration.config.sqs;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.amazonaws.services.sqs.model.ListQueuesResult;
import com.oslash.integration.utils.Constants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessageQueConfiguration {
    @Value("${aws.sqs.endpoint}")
    String endPoint;

    @Value("${aws.region}")
    String awsRegion;

    @Value("${aws.accessKey}")
    String accessKey;

    @Value("${aws.secretKey}")
    String secretKey;

    @Bean
    public AmazonSQSAsync amazonSQSAsync() {
        final AwsClientBuilder.EndpointConfiguration endpointConfiguration =
            new AwsClientBuilder.EndpointConfiguration(endPoint, awsRegion);
        final AmazonSQSAsync sqsAsync = AmazonSQSAsyncClientBuilder
            .standard()
            .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)))
            .withEndpointConfiguration(endpointConfiguration)
            .build();
        final ListQueuesResult listQueuesResult = sqsAsync.listQueues(Constants.QUEUE_NAME);
        if (listQueuesResult.getQueueUrls().isEmpty()) {
            sqsAsync.createQueueAsync(Constants.QUEUE_NAME);
        }
        return sqsAsync;
    }
}
