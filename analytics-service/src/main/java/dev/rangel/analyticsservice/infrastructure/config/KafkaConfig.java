package dev.rangel.analyticsservice.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic redirectAttemptsDltTopic() {
        return TopicBuilder.name("redirect-attempts.DLT")
                .partitions(6)
                .replicas(1)
                .build();
    }

    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);

        FixedBackOff backOff = new FixedBackOff(1000L, 3L);

        return new DefaultErrorHandler(recoverer, backOff);
    }
}