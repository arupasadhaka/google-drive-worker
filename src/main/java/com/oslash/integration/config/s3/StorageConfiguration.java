package com.oslash.integration.config.s3;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The type Storage configuration.
 */
@Configuration
public class StorageConfiguration {

    @Value("${aws.region}")
    private String region;

    @Value("${aws.accessKey}")
    private String accessKey;

    @Value("${aws.secretKey}")
    private String secretKey;


    @Value("${aws.s3.endpoint}")
    private String s3EndPoint;


    /**
     * Amazon s 3 amazon s 3.
     *
     * @return the amazon s 3
     */
    @Bean
    public AmazonS3 amazonS3() {
        BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        AwsClientBuilder.EndpointConfiguration config = new AwsClientBuilder.EndpointConfiguration(s3EndPoint, region);
        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard();
        builder.withEndpointConfiguration(config);
        builder.withPathStyleAccessEnabled(true);
        builder.withCredentials(new AWSStaticCredentialsProvider(credentials));
        return builder.build();
    }
}

