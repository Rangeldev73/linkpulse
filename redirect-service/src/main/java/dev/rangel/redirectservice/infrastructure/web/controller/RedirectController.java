package dev.rangel.redirectservice.infrastructure.web.controller;

import dev.rangel.redirectservice.application.dto.RedirectResult;
import dev.rangel.redirectservice.application.service.RedirectResolutionService;
import dev.rangel.redirectservice.infrastructure.messaging.ClickEventPublisher;
import jakarta.servlet.http.HttpServletRequest;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RedirectController {

    private final RedirectResolutionService resolutionService;
    private final ClickEventPublisher clickEventPublisher;
    private final MeterRegistry meterRegistry;

    @GetMapping("/{code}")
    public ResponseEntity<Void> redirect(@PathVariable("code") String code, HttpServletRequest request) {
        RedirectResult result = resolutionService.resolve(code);

        String clientIp = extractClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        String referer = request.getHeader("Referer");

        clickEventPublisher.publish(code, result, clientIp, userAgent, referer);
        recordOutcome(result);

        return switch (result) {
            case RedirectResult.Success success ->
                    ResponseEntity.status(HttpStatus.FOUND)
                            .cacheControl(CacheControl.noStore())
                            .location(success.targetUri())
                            .build();

            case RedirectResult.Failure failure ->
                    ResponseEntity.notFound().build();
        };
    }

    /**
     * Business metric: redirect attempts by outcome. The tag values are a closed set
     * (success + the RedirectFailureReason enum), so cardinality stays bounded.
     * The short code is deliberately NOT a tag: it is unbounded and would explode the series count.
     */
    private void recordOutcome(RedirectResult result) {
        String outcome = switch (result) {
            case RedirectResult.Success success -> "success";
            case RedirectResult.Failure failure -> failure.reason().name().toLowerCase();
        };
        meterRegistry.counter("linkpulse.redirects", "outcome", outcome).increment();
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}