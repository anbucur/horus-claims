package com.msig.claimsapi.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic claimsFnohlIngestTopic() {
        return TopicBuilder.name("claims-fnohl-ingest")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic claimsValidationTopic() {
        return TopicBuilder.name("claims-validation")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic claimsAuditTopic() {
        return TopicBuilder.name("claims-audit")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic claimsStatusTopic() {
        return TopicBuilder.name("claims-status")
                .partitions(3)
                .replicas(1)
                .build();
    }
}