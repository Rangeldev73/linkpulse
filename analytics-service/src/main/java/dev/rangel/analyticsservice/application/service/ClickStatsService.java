package dev.rangel.analyticsservice.application.service;

import dev.rangel.analyticsservice.application.dto.ClickBucket;
import dev.rangel.analyticsservice.application.dto.ClickStatsResponse;
import dev.rangel.analyticsservice.application.dto.Granularity;
import dev.rangel.analyticsservice.application.dto.InvalidStatsQueryException;
import dev.rangel.analyticsservice.infrastructure.persistence.repository.ClickStatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClickStatsService {

    private final ClickStatsRepository clickStatsRepository;
    private final Clock clock;

    public ClickStatsResponse getClicks(String ownerId, String code, String granularity, Instant from, Instant to) {
        Granularity parsedGranularity = Granularity.from(granularity);

        Instant resolvedTo = (to != null) ? to : Instant.now(clock);
        Instant resolvedFrom = (from != null) ? from : resolvedTo.minus(parsedGranularity.defaultWindow());

        if (!resolvedFrom.isBefore(resolvedTo)) {
            throw new InvalidStatsQueryException("'from' must be strictly before 'to'");
        }

        if (Duration.between(resolvedFrom, resolvedTo).compareTo(parsedGranularity.maxWindow()) > 0) {
            throw new InvalidStatsQueryException(
                    "Requested window exceeds maximum allowed for granularity " + parsedGranularity.unit()
            );
        }

        List<ClickBucket> buckets = clickStatsRepository
                .countClicksByBucket(code, ownerId, parsedGranularity.unit(), resolvedFrom, resolvedTo)
                .stream()
                .map(row -> new ClickBucket(Instant.ofEpochSecond(row.getBucket()), row.getClicks()))
                .toList();

        return new ClickStatsResponse(
                code,
                parsedGranularity.unit(),
                resolvedFrom,
                resolvedTo,
                buckets
        );
    }
}