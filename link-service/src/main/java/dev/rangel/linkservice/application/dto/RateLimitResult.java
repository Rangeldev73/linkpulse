package dev.rangel.linkservice.application.dto;

/**
 * Outcome of a rate limit check.
 *
 * @param allowed           whether the request may proceed
 * @param retryAfterSeconds when rejected, how long the caller should wait; 0 when allowed
 */
public record RateLimitResult(boolean allowed, long retryAfterSeconds) {

    public static RateLimitResult permitted() {
        return new RateLimitResult(true, 0);
    }

    public static RateLimitResult rejected(long retryAfterSeconds) {
        return new RateLimitResult(false, retryAfterSeconds);
    }
}
