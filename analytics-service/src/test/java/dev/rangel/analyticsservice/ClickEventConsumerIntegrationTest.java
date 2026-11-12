package dev.rangel.analyticsservice;

import dev.rangel.analyticsservice.application.dto.RedirectAttemptEvent;
import dev.rangel.analyticsservice.infrastructure.messaging.ClickEventConsumer;
import dev.rangel.analyticsservice.infrastructure.persistence.repository.ClickEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClickEventConsumerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ClickEventConsumer clickEventConsumer;

    @Autowired
    private ClickEventRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("Scenario A: Consumer successfully processes and persists a valid success event with full mapping")
    void shouldConsumeAndSaveValidSuccessEvent() {
        Instant fixedInstant = Instant.parse("2026-11-08T10:00:00Z").truncatedTo(ChronoUnit.MICROS);
        String eventId = UUID.randomUUID().toString();
        var event = new RedirectAttemptEvent(
                eventId,
                "hot-001",
                "SUCCESS",
                "https://github.com",
                null,
                "0:0:0:0:0:0:0:1",
                "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X)",
                "https://twitter.com",
                fixedInstant
        );

        clickEventConsumer.consume(event);

        var savedEvents = repository.findAll();
        assertThat(savedEvents).hasSize(1);

        var saved = savedEvents.get(0);
        assertThat(saved.getEventId()).isEqualTo(eventId);
        assertThat(saved.getCode()).isEqualTo("hot-001");
        assertThat(saved.getStatus()).isEqualTo("SUCCESS");
        assertThat(saved.getTargetUrl()).isEqualTo("https://github.com");
        assertThat(saved.getClientIp()).isEqualTo("0:0:0:0:0:0:0:1");
        assertThat(saved.getUserAgent()).contains("iPhone");
        assertThat(saved.getReferer()).isEqualTo("https://twitter.com");
        assertThat(saved.getOccurredAt()).isEqualTo(fixedInstant);
    }

    @Test
    @DisplayName("Scenario B: Consumer successfully processes and persists an event with null optional fields")
    void shouldConsumeAndSaveEventWithNullOptionalFields() {
        Instant fixedInstant = Instant.parse("2026-11-08T10:00:00Z").truncatedTo(ChronoUnit.MICROS);
        var event = new RedirectAttemptEvent(
                UUID.randomUUID().toString(),
                "hot-001",
                "SUCCESS",
                "https://github.com",
                null,
                "0:0:0:0:0:0:0:1",
                null,
                null,
                fixedInstant
        );

        clickEventConsumer.consume(event);

        var savedEvents = repository.findAll();
        assertThat(savedEvents).hasSize(1);
        assertThat(savedEvents.get(0).getUserAgent()).isNull();
        assertThat(savedEvents.get(0).getReferer()).isNull();
    }

    @Test
    @DisplayName("Scenario C: Consumer correctly processes and persists a failure event with mapped fields")
    void shouldConsumeAndSaveFailureEvent() {
        Instant fixedInstant = Instant.parse("2026-11-08T10:00:00Z").truncatedTo(ChronoUnit.MICROS);
        var event = new RedirectAttemptEvent(
                UUID.randomUUID().toString(),
                "invalid-code",
                "FAILURE",
                null,
                "NOT_FOUND",
                "0:0:0:0:0:0:0:1",
                null,
                null,
                fixedInstant
        );

        clickEventConsumer.consume(event);

        var savedEvents = repository.findAll();
        assertThat(savedEvents).hasSize(1);
        assertThat(savedEvents.get(0).getCode()).isEqualTo("invalid-code");
        assertThat(savedEvents.get(0).getStatus()).isEqualTo("FAILURE");
        assertThat(savedEvents.get(0).getTargetUrl()).isNull();
        assertThat(savedEvents.get(0).getFailureReason()).isEqualTo("NOT_FOUND");
    }

    @Test
    @DisplayName("Scenario D: Idempotency - Consumer handles duplicate event gracefully without throwing or duplicating")
    void shouldHandleDuplicateEventGracefully() {
        Instant fixedInstant = Instant.parse("2026-11-08T10:00:00Z").truncatedTo(ChronoUnit.MICROS);
        String eventId = UUID.randomUUID().toString();
        var event = new RedirectAttemptEvent(
                eventId,
                "hot-001",
                "SUCCESS",
                "https://github.com",
                null,
                "0:0:0:0:0:0:0:1",
                "Mozilla/5.0",
                null,
                fixedInstant
        );

        clickEventConsumer.consume(event);
        assertThat(repository.count()).isEqualTo(1);

        clickEventConsumer.consume(event);
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Scenario E: Unrelated database violation propagates error with positive root cause check")
    void shouldPropagateNonDuplicateDatabaseViolations() {
        Instant fixedInstant = Instant.parse("2026-11-08T10:00:00Z").truncatedTo(ChronoUnit.MICROS);
        String longCode = "a".repeat(51); // Column code is VARCHAR(50)
        var event = new RedirectAttemptEvent(
                UUID.randomUUID().toString(),
                longCode,
                "SUCCESS",
                "https://github.com",
                null,
                "0:0:0:0:0:0:0:1",
                null,
                null,
                fixedInstant
        );

        assertThatThrownBy(() -> clickEventConsumer.consume(event))
                .isInstanceOf(DataIntegrityViolationException.class)
                .rootCause()
                .hasMessageContaining("character varying(50)");
    }
}