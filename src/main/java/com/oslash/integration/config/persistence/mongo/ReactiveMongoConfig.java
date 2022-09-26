package com.oslash.integration.config.persistence.mongo;

import com.mongodb.reactivestreams.client.MongoClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

/**
 * The type Reactive mongo config.
 */
@Configuration
public class ReactiveMongoConfig {
    /**
     * The Database.
     */
    @Value("${spring.data.mongodb.database}")
    String database;

    /**
     * The Mongo client.
     */
    @Autowired
    MongoClient mongoClient;

    /**
     * Reactive mongo template reactive mongo template.
     *
     * @return the reactive mongo template
     */
    @Bean
    public ReactiveMongoTemplate reactiveMongoTemplate() {
        return new ReactiveMongoTemplate(mongoClient, database);
    }
}