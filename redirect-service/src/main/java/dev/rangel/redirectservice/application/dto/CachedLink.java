package dev.rangel.redirectservice.application.dto;

import java.time.Instant;

public record CachedLink(
        String code,
        String originalUrl,
        String ownerId,
        Instant expiresAt,
        boolean isActive
) {}