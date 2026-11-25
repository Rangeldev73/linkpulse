package dev.rangel.analyticsservice.application.dto;

import java.time.Instant;
import java.util.List;

public record ClickStatsResponse(
        String code,
        String granularity,
        Instant from,
        Instant to,
        List<ClickBucket> buckets
) {}