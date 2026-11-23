package com.example.sis3.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic bookEventsTopic(@Value("${app.kafka.topics.book-events}") String topicName) {
        return TopicBuilder.name(topicName)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic bookEventsDltTopic(@Value("${app.kafka.topics.book-events-dlt}") String dltTopicName) {
        return TopicBuilder.name(dltTopicName)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public CommonErrorHandler kafkaErrorHandler(
            KafkaOperations<String, String> kafkaOperations,
            @Value("${app.kafka.topics.book-events-dlt}") String dltTopicName) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaOperations,
                (record, ex) -> new TopicPartition(dltTopicName, record.partition())
        );
        return new DefaultErrorHandler(recoverer, new FixedBackOff(500L, 2L));
    }
}