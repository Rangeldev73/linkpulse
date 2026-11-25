package dev.rangel.analyticsservice;

import dev.rangel.analyticsservice.domain.model.ClickEventEntity;
import dev.rangel.analyticsservice.infrastructure.persistence.repository.ClickBucketProjection;
import dev.rangel.analyticsservice.infrastructure.persistence.repository.ClickEventRepository;
import dev.rangel.analyticsservice.infrastructure.persistence.repository.ClickStatsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class ClickStatsRepositoryTest extends BaseIntegrationTest {

    private static final Instant START = Instant.parse("2026-11-10T10:00:00Z");
    private static final Instant END = Instant.parse("2026-11-10T12:00:00Z");

    @Autowired
    private ClickStatsRepository statsRepository;

    @Autowired
    private ClickEventRepository eventRepository;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
    }

    @Test
    @DisplayName("Groups successful clicks of one code into hourly buckets, ordered by time")
    void shouldGroupClicksByHour() {
        save("stats-001", "SUCCESS", "2026-11-10T10:05:00Z");
        save("stats-001", "SUCCESS", "2026-11-10T10:55:00Z");
        save("stats-001", "SUCCESS", "2026-11-10T11:10:00Z");

        var buckets = statsRepository.countClicksByBucket("stats-001", "hour", START, END);

        assertThat(buckets)
                .extracting(ClickBucketProjection::getBucket, ClickBucketProjection::getClicks)
                .containsExactly(
                        tuple(epoch("2026-11-10T10:00:00Z"), 2L),
                        tuple(epoch("2026-11-10T11:00:00Z"), 1L));
    }

    @Test
    @DisplayName("Ignores failed attempts and events of other codes")
    void shouldCountOnlySuccessfulClicksOfTheRequestedCode() {
        save("stats-001", "SUCCESS", "2026-11-10T10:05:00Z");
        save("stats-001", "FAILURE", "2026-11-10T10:30:00Z");
        save("stats-002", "SUCCESS", "2026-11-10T10:40:00Z");

        var buckets = statsRepository.countClicksByBucket("stats-001", "hour", START, END);

        assertThat(buckets)
                .extracting(ClickBucketProjection::getBucket, ClickBucketProjection::getClicks)
                .containsExactly(tuple(epoch("2026-11-10T10:00:00Z"), 1L));
    }

    @Test
    @DisplayName("Window is closed at the start and open at the end")
    void shouldIncludeStartAndExcludeEndBoundary() {
        save("stats-001", "SUCCESS", "2026-11-10T10:00:00Z");
        save("stats-001", "SUCCESS", "2026-11-10T12:00:00Z");

        var buckets = statsRepository.countClicksByBucket("stats-001", "hour", START, END);

        assertThat(buckets)
                .extracting(ClickBucketProjection::getBucket, ClickBucketProjection::getClicks)
                .containsExactly(tuple(epoch("2026-11-10T10:00:00Z"), 1L));
    }

    @Test
    @DisplayName("Groups clicks into UTC daily buckets")
    void shouldGroupClicksByDay() {
        save("stats-001", "SUCCESS", "2026-11-10T01:00:00Z");
        save("stats-001", "SUCCESS", "2026-11-10T23:30:00Z");
        save("stats-001", "SUCCESS", "2026-11-11T00:30:00Z");

        var buckets = statsRepository.countClicksByBucket("stats-001", "day",
                Instant.parse("2026-11-10T00:00:00Z"), Instant.parse("2026-11-12T00:00:00Z"));

        assertThat(buckets)
                .extracting(ClickBucketProjection::getBucket, ClickBucketProjection::getClicks)
                .containsExactly(
                        tuple(epoch("2026-11-10T00:00:00Z"), 2L),
                        tuple(epoch("2026-11-11T00:00:00Z"), 1L));
    }

    @Test
    @DisplayName("Returns an empty list when the code has no clicks in the window")
    void shouldReturnEmptyListWhenNoClicks() {
        assertThat(statsRepository.countClicksByBucket("stats-001", "hour", START, END)).isEmpty();
    }

    private void save(String code, String status, String occurredAt) {
        eventRepository.save(ClickEventEntity.builder()
                .eventId(UUID.randomUUID().toString())
                .code(code)
                .status(status)
                .failureReason("FAILURE".equals(status) ? "NOT_FOUND" : null)
                .occurredAt(Instant.parse(occurredAt))
                .build());
    }

    private static long epoch(String instant) {
        return Instant.parse(instant).getEpochSecond();
    }
}
