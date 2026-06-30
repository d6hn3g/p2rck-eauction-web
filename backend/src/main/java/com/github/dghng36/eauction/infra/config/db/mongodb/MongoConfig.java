package com.github.dghng36.eauction.infra.config.db.mongodb;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.transaction.PlatformTransactionManager;

import com.github.dghng36.eauction.infra.config.db.IDatabaseConfig;

@Configuration
@EnableMongoAuditing
public class MongoConfig implements IDatabaseConfig{
    private final MongoDatabaseFactory dbFactory;

    public MongoConfig(MongoDatabaseFactory dbFactory) {
        this.dbFactory = dbFactory;
    }

    @Bean
    @Override
    public PlatformTransactionManager transactionManager() {
        return new MongoTransactionManager(dbFactory);
    }
}
