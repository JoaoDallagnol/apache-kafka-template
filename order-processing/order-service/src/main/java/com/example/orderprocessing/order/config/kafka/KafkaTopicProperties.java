package com.example.orderprocessing.order.config.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka.topics")
public record KafkaTopicProperties(Topic ordersCreated) {
    public record Topic(
            String name,
            int partitions,
            short replicas
    ) {}
}
