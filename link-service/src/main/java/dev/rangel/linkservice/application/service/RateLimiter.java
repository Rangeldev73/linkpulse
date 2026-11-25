package dev.rangel.linkservice.application.service;

import dev.rangel.linkservice.application.dto.RateLimitResult;

import java.time.Duration;

public interface RateLimiter {

    /**
     * Counts one attempt for the given key and decides whether it is within the limit.
     *
     * @param key    identifies who is being limited (e.g. "rate-limit:link-creation:user-1")
     * @param limit  maximum attempts allowed within the window
     * @param window length of the counting window
     */
    RateLimitResult tryAcquire(String key, int limit, Duration window);
}
