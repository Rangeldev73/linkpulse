package dev.rangel.redirectservice.application.dto;

import java.time.Instant;

public record RedirectAttemptEvent(
        String eventId,
        String code,
        String status,
        String targetUrl,
        String failureReason,
        String clientIp,
        String userAgent,
        String referer,
        Instant timestamp
) {}