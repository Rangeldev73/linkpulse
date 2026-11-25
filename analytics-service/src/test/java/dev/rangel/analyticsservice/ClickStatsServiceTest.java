package dev.rangel.analyticsservice;

import dev.rangel.analyticsservice.application.dto.ClickStatsResponse;
import dev.rangel.analyticsservice.application.dto.InvalidStatsQueryException;
import dev.rangel.analyticsservice.application.service.ClickStatsService;
import dev.rangel.analyticsservice.infrastructure.persistence.repository.ClickBucketProjection;
import dev.rangel.analyticsservice.infrastructure.persistence.repository.ClickStatsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClickStatsServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-20T12:00:00Z");

    @Mock
    private ClickStatsRepository repository;

    private ClickStatsService service;

    @BeforeEach
    void setUp() {
        service = new ClickStatsService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("Hourly query without a window defaults to the last 24 hours")
    void shouldDefaultHourlyWindowToLast24Hours() {
        when(repository.countClicksByBucket(anyString(), anyString(), any(), any())).thenReturn(List.of());

        ClickStatsResponse response = service.getClicks("hot-001", "hour", null, null);

        assertThat(response.to()).isEqualTo(NOW);
        assertThat(response.from()).isEqualTo(NOW.minus(Duration.ofHours(24)));
        assertThat(response.granularity()).isEqualTo("hour");
        verify(repository).countClicksByBucket("hot-001", "hour", NOW.minus(Duration.ofHours(24)), NOW);
    }

    @Test
    @DisplayName("Daily query without a window defaults to the last 30 days")
    void shouldDefaultDailyWindowToLast30Days() {
        when(repository.countClicksByBucket(anyString(), anyString(), any(), any())).thenReturn(List.of());

        ClickStatsResponse response = service.getClicks("hot-001", "day", null, null);

        assertThat(response.from()).isEqualTo(NOW.minus(Duration.ofDays(30)));
        verify(repository).countClicksByBucket("hot-001", "day", NOW.minus(Duration.ofDays(30)), NOW);
    }

    @Test
    @DisplayName("Granularity is case-insensitive")
    void shouldAcceptUpperCaseGranularity() {
        when(repository.countClicksByBucket(anyString(), anyString(), any(), any())).thenReturn(List.of());

        assertThat(service.getClicks("hot-001", "HOUR", null, null).granularity()).isEqualTo("hour");
    }

    @Test
    @DisplayName("Maps repository rows (epoch seconds) to buckets with Instants")
    void shouldMapRowsToBuckets() {
        Instant start = Instant.parse("2026-11-20T10:00:00Z");
        when(repository.countClicksByBucket(anyString(), anyString(), any(), any()))
                .thenReturn(List.of(row(start.getEpochSecond(), 2L), row(start.plusSeconds(3600).getEpochSecond(), 1L)));

        ClickStatsResponse response = service.getClicks("hot-001", "hour", start, start.plusSeconds(7200));

        assertThat(response.buckets()).hasSize(2);
        assertThat(response.buckets().get(0).bucket()).isEqualTo(start);
        assertThat(response.buckets().get(0).clicks()).isEqualTo(2L);
        assertThat(response.buckets().get(1).bucket()).isEqualTo(start.plusSeconds(3600));
        assertThat(response.buckets().get(1).clicks()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Rejects 'from' equal to or after 'to'")
    void shouldRejectInvertedOrEmptyWindow() {
        assertThatThrownBy(() -> service.getClicks("hot-001", "hour", NOW, NOW))
                .isInstanceOf(InvalidStatsQueryException.class);
        assertThatThrownBy(() -> service.getClicks("hot-001", "hour", NOW.plusSeconds(1), NOW))
                .isInstanceOf(InvalidStatsQueryException.class);
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Rejects a window larger than the maximum, but accepts exactly the maximum")
    void shouldEnforceMaximumWindow() {
        Instant to = NOW;
        assertThatThrownBy(() -> service.getClicks("hot-001", "hour", to.minus(Duration.ofDays(7)).minusSeconds(1), to))
                .isInstanceOf(InvalidStatsQueryException.class);

        when(repository.countClicksByBucket(anyString(), anyString(), any(), any())).thenReturn(List.of());
        assertThatCode(() -> service.getClicks("hot-001", "hour", to.minus(Duration.ofDays(7)), to))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Rejects unsupported granularity before touching the database")
    void shouldRejectUnsupportedGranularity() {
        assertThatThrownBy(() -> service.getClicks("hot-001", "minute", null, null))
                .isInstanceOf(InvalidStatsQueryException.class)
                .hasMessageContaining("minute");
        verifyNoInteractions(repository);
    }

    private static ClickBucketProjection row(long bucket, long clicks) {
        return new ClickBucketProjection() {
            @Override
            public Long getBucket() {
                return bucket;
            }

            @Override
            public Long getClicks() {
                return clicks;
            }
        };
    }
}
