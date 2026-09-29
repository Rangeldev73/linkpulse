package dev.rangel.linkservice.application.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.time.Instant;

public record CreateLinkRequest(
        @NotBlank(message = "Original URL is required")
        @URL(message = "Must be a valid URL")
        String originalUrl,

        @Size(min = 3, max = 50, message = "Custom alias must be between 3 and 50 characters")
        @Pattern(
                regexp = "^[a-zA-Z0-9_-]+$",
                message = "Custom alias can only contain alphanumeric characters, hyphens, and underscores"
        )
        String customAlias,

        @Future(message = "Expiration date must be in the future")
        Instant expiresAt
) {}