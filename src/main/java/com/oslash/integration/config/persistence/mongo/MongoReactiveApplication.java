package com.oslash.integration.config.persistence.mongo;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.config.AbstractReactiveMongoConfiguration;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

/**
 * The type Mongo reactive application.
 */
@EnableReactiveMongoRepositories
public class MongoReactiveApplication extends AbstractReactiveMongoConfiguration {
    /**
     * The Database.
     */
    @Value("${spring.data.mongodb.database}")
    String database;

    /**
     * Mongo client mongo client.
     *
     * @return the mongo client
     */
    @Bean
    public MongoClient mongoClient() {
        return MongoClients.create();
    }

    /**
     * Gets database name.
     *
     * @return the database name
     */
    @Override
    protected String getDatabaseName() {
        return database;
    }
}