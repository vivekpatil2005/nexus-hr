package com.nexushr.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * JPA entity representing a refresh token for JWT token rotation.
 * <p>
 * Each login creates a new token family. When a refresh token is used, the old one is revoked
 * and a new one is created in the same family. If a revoked token is reused, the entire family
 * is revoked as a security measure against token theft.
 * </p>
 */
@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_refresh_tokens_username", columnList = "username"),
        @Index(name = "idx_refresh_tokens_family", columnList = "family")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    /** Secure random UUID used as the token value. */
    @Id
    @Column(length = 64)
    private String token;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false)
    private Instant expiryDate;

    @Column(nullable = false)
    private boolean revoked;

    /**
     * Token family identifier for reuse detection.
     * All tokens derived from the same login session share the same family UUID.
     */
    @Column(nullable = false, length = 64)
    private String family;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Checks whether this refresh token has expired.
     *
     * @return {@code true} if the token's expiry date is before the current instant
     */
    public boolean isExpired() {
        return expiryDate.isBefore(Instant.now());
    }

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
