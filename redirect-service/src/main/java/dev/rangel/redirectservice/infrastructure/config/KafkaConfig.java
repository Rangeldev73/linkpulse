package dev.rangel.redirectservice.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic redirectAttemptsTopic() {
        return TopicBuilder.name("redirect-attempts")
                .partitions(6)
                .replicas(1)
                .build();
    }
}