package dev.rangel.redirectservice.application.service;

import dev.rangel.redirectservice.application.dto.CachedLink;
import dev.rangel.redirectservice.domain.model.RedirectableLink;
import dev.rangel.redirectservice.domain.repository.RedirectableLinkRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CachedLinkLookupService {

    private final RedirectableLinkRepository repository;
    private final RedisTemplate<String, CachedLink> redisTemplate;
    private static final Logger log = LoggerFactory.getLogger(CachedLinkLookupService.class);

    private static final String CACHE_KEY_PREFIX = "link:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(3);

    private static final CachedLink SENTINEL = new CachedLink(
            "__NOT_FOUND__",
            "",
            Instant.EPOCH,
            false
    );

    public Optional<CachedLink> getLink(String code) {
        String cacheKey = CACHE_KEY_PREFIX + code;

        CachedLink cachedValue = redisTemplate.opsForValue().get(cacheKey);

        if (cachedValue != null) {
            if (SENTINEL.code().equals(cachedValue.code())) {
                log.debug("Cache hit (Negative): Code {} resolved from Redis as NOT_FOUND", code);
                return Optional.empty();
            }
            log.debug("Cache hit (Positive): Code {} resolved from Redis", code);
            return Optional.of(cachedValue);
        }

        log.debug("Cache miss: Fetching code {} from PostgreSQL", code);
        Optional<RedirectableLink> dbLink = repository.findByCode(code);

        if (dbLink.isEmpty()) {
            redisTemplate.opsForValue().set(cacheKey, SENTINEL, CACHE_TTL);
            return Optional.empty();
        }

        CachedLink newCacheEntry = mapToDto(dbLink.get());
        redisTemplate.opsForValue().set(cacheKey, newCacheEntry, CACHE_TTL);

        return Optional.of(newCacheEntry);
    }

    private CachedLink mapToDto(RedirectableLink entity) {
        return new CachedLink(
                entity.getCode(),
                entity.getOriginalUrl(),
                entity.getExpiresAt(),
                entity.getIsActive()
        );
    }
}