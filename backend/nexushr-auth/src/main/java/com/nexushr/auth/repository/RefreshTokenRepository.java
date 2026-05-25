package com.nexushr.auth.repository;

import com.nexushr.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link RefreshToken} entity.
 * Supports token lookup, family-based revocation, and cleanup of expired tokens.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    /**
     * Finds a refresh token by its token value.
     *
     * @param token the token string
     * @return an {@link Optional} containing the token if found
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Deletes all refresh tokens belonging to a specific user.
     * Used during logout to invalidate all active sessions.
     *
     * @param username the username whose tokens should be deleted
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.username = :username")
    void deleteByUsername(@Param("username") String username);

    /**
     * Revokes all refresh tokens in a given token family.
     * This is a security measure triggered when token reuse is detected,
     * indicating potential token theft.
     *
     * @param family the token family identifier
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.family = :family")
    void revokeAllByFamily(@Param("family") String family);

    /**
     * Deletes all expired and revoked tokens for housekeeping.
     *
     * @param now the current instant to compare against expiry dates
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiryDate < :now OR rt.revoked = true")
    void deleteExpiredAndRevoked(@Param("now") Instant now);
}
