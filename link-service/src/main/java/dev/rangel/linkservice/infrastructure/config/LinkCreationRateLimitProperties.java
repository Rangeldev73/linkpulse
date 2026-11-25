package dev.rangel.linkservice.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.rate-limit.link-creation")
public record LinkCreationRateLimitProperties(
        @DefaultValue("10") int limit,
        @DefaultValue("60s") Duration window
) {}
