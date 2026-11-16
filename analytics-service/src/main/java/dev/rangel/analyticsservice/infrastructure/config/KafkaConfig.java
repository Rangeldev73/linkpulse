package dev.rangel.analyticsservice.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    public static final String DLT_TOPIC = "redirect-attempts.DLT";

    @Bean
    public NewTopic redirectAttemptsDltTopic() {
        return TopicBuilder.name(DLT_TOPIC)
                .partitions(6)
                .replicas(1)
                .build();
    }

    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> jsonTemplate,
                                            ProducerFactory<Object, Object> producerFactory) {
        // Messages that failed deserialization are republished as their original raw bytes,
        // so they need a template with a ByteArraySerializer (JsonSerializer would mangle them).
        // Deliberately NOT a @Bean: a second KafkaTemplate bean would disable Boot's auto-configured one.
        Map<String, Object> bytesProps = new HashMap<>(producerFactory.getConfigurationProperties());
        KafkaTemplate<String, byte[]> bytesTemplate = new KafkaTemplate<>(
                new DefaultKafkaProducerFactory<>(bytesProps, new StringSerializer(), new ByteArraySerializer()));

        // Order matters: the recoverer picks the first entry whose class accepts the value.
        Map<Class<?>, KafkaOperations<?, ?>> templates = new LinkedHashMap<>();
        templates.put(byte[].class, bytesTemplate);
        templates.put(Object.class, jsonTemplate);

        // Explicit destination: Spring Kafka's default DLT suffix is "-dlt" in current versions
        // (it used to be ".DLT"), which silently sent records to a topic nobody provisioned.
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                templates, (record, ex) -> new TopicPartition(DLT_TOPIC, record.partition()));
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2L));
    }
}