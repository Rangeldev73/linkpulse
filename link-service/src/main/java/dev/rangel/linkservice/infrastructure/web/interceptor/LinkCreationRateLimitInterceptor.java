package dev.rangel.linkservice.infrastructure.web.interceptor;

import dev.rangel.linkservice.application.dto.RateLimitResult;
import dev.rangel.linkservice.application.service.RateLimiter;
import dev.rangel.linkservice.domain.exception.RateLimitExceededException;
import dev.rangel.linkservice.infrastructure.config.LinkCreationRateLimitProperties;
import jakarta.servlet.http.HttpServletRequest;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Limits link creation per authenticated user. Runs after the security filter chain,
 * so the user is always known here (unauthenticated requests never reach it).
 */
@Component
@RequiredArgsConstructor
public class LinkCreationRateLimitInterceptor implements HandlerInterceptor {

    private static final String KEY_PREFIX = "rate-limit:link-creation:";

    private final RateLimiter rateLimiter;
    private final LinkCreationRateLimitProperties properties;
    private final MeterRegistry meterRegistry;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return true;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        RateLimitResult result = rateLimiter.tryAcquire(
                KEY_PREFIX + authentication.getName(), properties.limit(), properties.window());

        if (!result.allowed()) {
            meterRegistry.counter("linkpulse.ratelimit.rejected", "endpoint", "link-creation").increment();
            throw new RateLimitExceededException(result.retryAfterSeconds());
        }
        return true;
    }
}
