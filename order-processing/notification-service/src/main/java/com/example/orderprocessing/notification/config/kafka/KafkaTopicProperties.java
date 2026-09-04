package com.example.orderprocessing.notification.config.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka.topics")
public record KafkaTopicProperties(
        Topic ordersCreated,
        Topic paymentsProcessed
) {
    public record Topic(String name) {
    }
}
