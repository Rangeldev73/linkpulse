package dev.rangel.redirectservice.application.service;

import dev.rangel.redirectservice.application.dto.CachedLink;
import dev.rangel.redirectservice.application.dto.RedirectFailureReason;
import dev.rangel.redirectservice.application.dto.RedirectResult;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RedirectResolutionService {

    private final CachedLinkLookupService lookupService;
    private final Clock clock;
    private static final Logger log = LoggerFactory.getLogger(RedirectResolutionService.class);

    public RedirectResult resolve(String shortCode) {
        return lookupService.getLink(shortCode)
                .map(this::evaluateStatus)
                .orElseGet(() -> logAndReturnFailure(shortCode, RedirectFailureReason.NOT_FOUND));
    }

    private RedirectResult evaluateStatus(CachedLink link) {
        if (!link.isActive()) {
            return logAndReturnFailure(link.code(), RedirectFailureReason.INACTIVE);
        }

        if (link.expiresAt() != null && link.expiresAt().isBefore(Instant.now(clock))) {
            return logAndReturnFailure(link.code(), RedirectFailureReason.EXPIRED);
        }

        try {
            URI targetUri = URI.create(link.originalUrl());
            return new RedirectResult.Success(targetUri);
        } catch (IllegalArgumentException e) {
            return logAndReturnFailure(link.code(), RedirectFailureReason.INVALID_URL);
        }
    }

    private RedirectResult logAndReturnFailure(String shortCode, RedirectFailureReason reason) {
        log.atWarn()
                .setMessage("Redirection failed")
                .addKeyValue("shortCode", shortCode)
                .addKeyValue("reason", reason.name())
                .log();

        return new RedirectResult.Failure(reason);
    }
}