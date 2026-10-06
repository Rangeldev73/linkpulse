package dev.rangel.redirectservice.application.service;

import dev.rangel.redirectservice.application.dto.RedirectFailureReason;
import dev.rangel.redirectservice.application.dto.RedirectResult;
import dev.rangel.redirectservice.domain.model.RedirectableLink;
import dev.rangel.redirectservice.domain.repository.RedirectableLinkRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RedirectResolutionService {

    private final RedirectableLinkRepository repository;
    private final Clock clock;
    private static final Logger log = LoggerFactory.getLogger(RedirectResolutionService.class);

    @Transactional(readOnly = true)
    public RedirectResult resolve(String shortCode) {
        return repository.findByCode(shortCode)
                .map(this::evaluateStatus)
                .orElseGet(() -> logAndReturnFailure(shortCode, RedirectFailureReason.NOT_FOUND));
    }

    private RedirectResult evaluateStatus(RedirectableLink link) {
        if (!link.getIsActive()) {
            return logAndReturnFailure(link.getCode(), RedirectFailureReason.INACTIVE);
        }

        if (link.getExpiresAt() != null && link.getExpiresAt().isBefore(Instant.now(clock))) {
            return logAndReturnFailure(link.getCode(), RedirectFailureReason.EXPIRED);
        }

        try {
            URI targetUri = URI.create(link.getOriginalUrl());
            return new RedirectResult.Success(targetUri);
        } catch (IllegalArgumentException e) {
            return logAndReturnFailure(link.getCode(), RedirectFailureReason.INVALID_URL);
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