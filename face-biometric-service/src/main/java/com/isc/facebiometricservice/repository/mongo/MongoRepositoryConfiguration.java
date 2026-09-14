package com.isc.facebiometricservice.repository.mongo;

import com.isc.facebiometricservice.config.BiometricProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.MongoDatabaseFactory;

@Configuration
@ConditionalOnProperty(name="biometric.repository-type", havingValue="mongo")
public class MongoRepositoryConfiguration {
    @Bean
    MongoDatabaseFactory mongoDatabaseFactory(BiometricProperties properties) {
        return new SimpleMongoClientDatabaseFactory(properties.mongo().uri());
    }

    @Bean
    MongoTemplate mongoTemplate(MongoDatabaseFactory factory) {
        return new MongoTemplate(factory);
    }
}
