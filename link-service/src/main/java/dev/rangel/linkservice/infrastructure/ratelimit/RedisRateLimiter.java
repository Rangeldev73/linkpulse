package dev.rangel.linkservice.infrastructure.ratelimit;

import dev.rangel.linkservice.application.dto.RateLimitResult;
import dev.rangel.linkservice.application.service.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Fixed-window counter shared by every instance through Redis.
 * <p>
 * INCR and EXPIRE run inside one Lua script, which Redis executes atomically. Done as two separate
 * commands, a crash between them would leave a counter with no expiry and block the user forever.
 * <p>
 * Fail-open: if Redis is unreachable the request is allowed. This limiter only protects against abuse;
 * it must not take link creation down with it.
 */
@Component
public class RedisRateLimiter implements RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimiter.class);

    private static final String COUNT_SCRIPT = """
            local current = redis.call('INCR', KEYS[1])
            local ttl = redis.call('TTL', KEYS[1])
            if current == 1 or ttl < 0 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
                ttl = tonumber(ARGV[1])
            end
            return {current, ttl}
            """;

    @SuppressWarnings("rawtypes")
    private final DefaultRedisScript<List> script = new DefaultRedisScript<>(COUNT_SCRIPT, List.class);

    private final StringRedisTemplate redisTemplate;

    public RedisRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public RateLimitResult tryAcquire(String key, int limit, Duration window) {
        try {
            List<?> result = redisTemplate.execute(script, List.of(key), String.valueOf(window.toSeconds()));
            long current = ((Number) result.get(0)).longValue();
            long ttl = ((Number) result.get(1)).longValue();

            if (current <= limit) {
                return RateLimitResult.permitted();
            }
            return RateLimitResult.rejected(Math.max(ttl, 1));
        } catch (DataAccessException e) {
            log.warn("Rate limiter unavailable, allowing the request (fail-open). key={} cause={}", key, e.getMessage());
            return RateLimitResult.permitted();
        }
    }
}
