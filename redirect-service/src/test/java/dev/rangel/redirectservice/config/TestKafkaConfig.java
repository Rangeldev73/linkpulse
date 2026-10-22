package dev.rangel.redirectservice.config;

import dev.rangel.redirectservice.application.dto.RedirectAttemptEvent;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@TestConfiguration
public class TestKafkaConfig {

    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public KafkaTemplate<String, RedirectAttemptEvent> kafkaTemplate() {
        KafkaTemplate<String, RedirectAttemptEvent> mockTemplate = Mockito.mock(KafkaTemplate.class);

        SendResult<String, RedirectAttemptEvent> sendResult = Mockito.mock(SendResult.class);
        RecordMetadata recordMetadata = new RecordMetadata(new TopicPartition("redirect-attempts", 0), 0L, 0, System.currentTimeMillis(), 0, 0);
        Mockito.when(sendResult.getRecordMetadata()).thenReturn(recordMetadata);

        Mockito.when(mockTemplate.send(anyString(), anyString(), any(RedirectAttemptEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        return mockTemplate;
    }
}