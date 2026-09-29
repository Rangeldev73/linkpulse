package dev.rangel.linkservice.application.dto;

import dev.rangel.linkservice.domain.model.Link;
import java.time.Instant;

public record LinkResponse(
        String shortUrl,
        String originalUrl,
        Instant expiresAt
) {
    public static LinkResponse from(Link link, String baseUrl) {
        String fullShortUrl = baseUrl.endsWith("/")
                ? baseUrl + link.getCode()
                : baseUrl + "/" + link.getCode();

        return new LinkResponse(
                fullShortUrl,
                link.getOriginalUrl(),
                link.getExpiresAt()
        );
    }
}