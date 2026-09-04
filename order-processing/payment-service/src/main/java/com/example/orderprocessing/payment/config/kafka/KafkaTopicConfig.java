package com.example.orderprocessing.payment.config.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@EnableConfigurationProperties(KafkaTopicProperties.class)
public class KafkaTopicConfig {

    private final KafkaTopicProperties properties;

    public KafkaTopicConfig(KafkaTopicProperties properties) {
        this.properties = properties;
    }

    @Bean
    public NewTopic paymentsProcessedTopic() {
        var topic = properties.paymentsProcessed();
        return TopicBuilder.name(topic.name())
                .partitions(topic.partitions())
                .replicas(topic.replicas())
                .build();
    }

    @Bean
    public NewTopic ordersCreatedDltTopic() {
        var topic = properties.ordersCreated();
        return TopicBuilder.name(topic.dltName())
                .partitions(topic.partitions())
                .replicas(topic.replicas())
                .build();
    }
}
