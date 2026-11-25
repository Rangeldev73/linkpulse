package dev.rangel.linkservice.infrastructure.ratelimit;

import dev.rangel.linkservice.application.dto.RateLimitResult;
import dev.rangel.linkservice.application.service.RateLimiter;
import dev.rangel.linkservice.config.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class RedisRateLimiterIntegrationTest extends AbstractIntegrationTest {

    private static final Duration WINDOW = Duration.ofSeconds(60);

    @Autowired
    private RateLimiter rateLimiter;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    @DisplayName("Allows attempts up to the limit, then rejects with a Retry-After inside the window")
    void shouldRejectOnceTheLimitIsExceeded() {
        for (int i = 0; i < 3; i++) {
            assertThat(rateLimiter.tryAcquire("rl:user-1", 3, WINDOW).allowed()).isTrue();
        }

        RateLimitResult rejected = rateLimiter.tryAcquire("rl:user-1", 3, WINDOW);

        assertThat(rejected.allowed()).isFalse();
        assertThat(rejected.retryAfterSeconds()).isBetween(1L, WINDOW.toSeconds());
    }

    @Test
    @DisplayName("Counters are independent per key")
    void shouldCountEachKeySeparately() {
        assertThat(rateLimiter.tryAcquire("rl:user-1", 1, WINDOW).allowed()).isTrue();
        assertThat(rateLimiter.tryAcquire("rl:user-1", 1, WINDOW).allowed()).isFalse();

        assertThat(rateLimiter.tryAcquire("rl:user-2", 1, WINDOW).allowed()).isTrue();
    }

    @Test
    @DisplayName("The counter always has an expiry, so a user can never be blocked forever")
    void shouldAlwaysSetAnExpiry() {
        rateLimiter.tryAcquire("rl:user-1", 3, WINDOW);

        Long ttl = redisTemplate.getExpire("rl:user-1");

        assertThat(ttl).isNotNull().isPositive().isLessThanOrEqualTo(WINDOW.toSeconds());
    }

    @Test
    @DisplayName("A new window starts once the previous one expires")
    void shouldAllowAgainAfterTheWindowExpires() {
        Duration shortWindow = Duration.ofSeconds(1);
        assertThat(rateLimiter.tryAcquire("rl:user-1", 1, shortWindow).allowed()).isTrue();
        assertThat(rateLimiter.tryAcquire("rl:user-1", 1, shortWindow).allowed()).isFalse();

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(rateLimiter.tryAcquire("rl:user-1", 1, shortWindow).allowed()).isTrue());
    }
}
