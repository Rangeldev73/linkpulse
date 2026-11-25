package dev.rangel.analyticsservice.application.dto;

import java.time.Instant;

public record ClickBucket(Instant bucket, long clicks) {}