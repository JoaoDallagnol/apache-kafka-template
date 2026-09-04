package com.example.orderprocessing.payment.config.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka.topics")
public record KafkaTopicProperties(
        Topic ordersCreated,
        Topic paymentsProcessed
) {
    public record Topic(
            String name,
            String dltName,
            int partitions,
            short replicas
    ) {
    }
}
