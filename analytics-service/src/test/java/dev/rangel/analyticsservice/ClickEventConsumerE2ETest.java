package dev.rangel.analyticsservice;

import dev.rangel.analyticsservice.domain.model.ClickEventEntity;
import dev.rangel.analyticsservice.infrastructure.persistence.repository.ClickEventRepository;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@TestPropertySource(properties = {
        "app.kafka.listener.auto-startup=true",
        "spring.kafka.admin.auto-create=true"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Testcontainers
class ClickEventConsumerE2ETest extends BaseIntegrationTest {

    private static final String TOPIC = "redirect-attempts";
    private static final String DLT_TOPIC = "redirect-attempts.DLT";
    private static final String KEY = "hot-001";
    private static final Instant FIXED_INSTANT = Instant.parse("2026-11-08T10:00:00Z");
    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    @Container
    @ServiceConnection
    static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka:3.8.0"));

    @TestConfiguration
    static class TopicConfig {
        @Bean
        public NewTopic redirectAttemptsTopic() {
            return TopicBuilder.name(TOPIC).partitions(6).replicas(1).build();
        }
    }

    @Autowired
    private ClickEventRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("Valid JSON published to Kafka is consumed and persisted in Postgres")
    void shouldPersistValidEventPublishedToKafka() throws Exception {
        String eventId = UUID.randomUUID().toString();

        publish(KEY, validEventJson(eventId, KEY));

        await().atMost(TIMEOUT).untilAsserted(() -> {
            List<ClickEventEntity> events = repository.findAll();
            assertThat(events).hasSize(1);
            ClickEventEntity saved = events.get(0);
            assertThat(saved.getEventId()).isEqualTo(eventId);
            assertThat(saved.getCode()).isEqualTo(KEY);
            assertThat(saved.getStatus()).isEqualTo("SUCCESS");
            assertThat(saved.getTargetUrl()).isEqualTo("https://github.com");
            assertThat(saved.getClientIp()).isEqualTo("0:0:0:0:0:0:0:1");
            assertThat(saved.getUserAgent()).contains("iPhone");
            assertThat(saved.getReferer()).isEqualTo("https://twitter.com");
            assertThat(saved.getOccurredAt()).isEqualTo(FIXED_INSTANT);
        });
    }

    @Test
    @DisplayName("Non-JSON message goes to the DLT with its original bytes and is never persisted")
    void shouldSendNonJsonMessageToDltPreservingOriginalPayload() throws Exception {
        String poison = "this is not json - " + UUID.randomUUID();

        publish(KEY, poison);

        ConsumerRecord<String, String> dltRecord = awaitDltRecordWithValue(poison);

        Header originalTopic = dltRecord.headers().lastHeader(KafkaHeaders.DLT_ORIGINAL_TOPIC);
        assertThat(originalTopic).isNotNull();
        assertThat(new String(originalTopic.value(), StandardCharsets.UTF_8)).isEqualTo(TOPIC);
        assertThat(repository.count()).isZero();
    }

    @Test
    @DisplayName("A poison message does not block the partition: the valid event after it is still persisted")
    void shouldKeepConsumingAfterPoisonMessageOnSamePartition() throws Exception {
        String eventId = UUID.randomUUID().toString();

        publish(KEY, "garbage before a valid event - " + UUID.randomUUID());
        publish(KEY, validEventJson(eventId, KEY));

        await().atMost(TIMEOUT).untilAsserted(() ->
                assertThat(repository.findAll())
                        .extracting(ClickEventEntity::getEventId)
                        .containsExactly(eventId));
    }

    @Test
    @DisplayName("A redelivered event (same event_id) is ignored and processing continues")
    void shouldIgnoreRedeliveredEventAndKeepProcessing() throws Exception {
        String firstId = UUID.randomUUID().toString();
        String secondId = UUID.randomUUID().toString();
        String firstJson = validEventJson(firstId, KEY);

        publish(KEY, firstJson);
        publish(KEY, firstJson);
        publish(KEY, validEventJson(secondId, KEY));

        await().atMost(TIMEOUT).untilAsserted(() ->
                assertThat(repository.findAll())
                        .extracting(ClickEventEntity::getEventId)
                        .containsExactlyInAnyOrder(firstId, secondId));
    }

    private void publish(String key, String value) throws Exception {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            producer.send(new ProducerRecord<>(TOPIC, key, value)).get(30, TimeUnit.SECONDS);
        }
    }

    private ConsumerRecord<String, String> awaitDltRecordWithValue(String expectedValue) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "dlt-reader-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        List<String> seen = new ArrayList<>();
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(DLT_TOPIC));
            long deadline = System.nanoTime() + TIMEOUT.toNanos();
            while (System.nanoTime() < deadline) {
                for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500))) {
                    if (expectedValue.equals(record.value())) {
                        return record;
                    }
                    seen.add(record.value());
                }
            }
        }
        throw new AssertionError("Original payload not found in " + DLT_TOPIC + ". Values seen: " + seen);
    }

    private static String validEventJson(String eventId, String code) {
        return """
                {"eventId":"%s","code":"%s","status":"SUCCESS","targetUrl":"https://github.com",\
                "failureReason":null,"clientIp":"0:0:0:0:0:0:0:1",\
                "userAgent":"Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X)",\
                "referer":"https://twitter.com","timestamp":"%s"}
                """.formatted(eventId, code, FIXED_INSTANT);
    }
}