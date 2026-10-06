package dev.rangel.redirectservice.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity
@Table(name = "tb_links")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class RedirectableLink {

    @Id
    private Long id;

    @Column(nullable = false, length = 50, unique = true)
    private String code;

    @Column(name = "original_url", nullable = false, columnDefinition = "TEXT")
    private String originalUrl;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
}