package com.nexushr.auth.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * JWT token provider responsible for generating, validating, and parsing JWT access tokens.
 * <p>
 * Uses HMAC-SHA512 signing with a configurable secret key. Access tokens are short-lived (15 minutes)
 * and contain the user's roles and userId as custom claims.
 * </p>
 *
 * <h3>Configuration Properties:</h3>
 * <ul>
 *   <li>{@code nexushr.jwt.secret} — Base64-encoded secret key (minimum 512 bits for HS512)</li>
 *   <li>{@code nexushr.jwt.access-token-expiration-ms} — Access token TTL in milliseconds (default: 900000 = 15 min)</li>
 *   <li>{@code nexushr.jwt.refresh-token-expiration-ms} — Refresh token TTL in milliseconds (default: 604800000 = 7 days)</li>
 * </ul>
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${nexushr.jwt.secret}")
    private String jwtSecret;

    @Value("${nexushr.jwt.access-token-expiration-ms:900000}")
    private long accessTokenExpirationMs;

    @Value("${nexushr.jwt.refresh-token-expiration-ms:604800000}")
    private long refreshTokenExpirationMs;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 64) {
            log.info("JWT secret is less than 512 bits. Hashing it with SHA-512 to ensure a secure key size.");
            try {
                java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-512");
                keyBytes = digest.digest(keyBytes);
            } catch (java.security.NoSuchAlgorithmException e) {
                log.error("SHA-512 digest algorithm not available. Falling back to key padding.", e);
                byte[] paddedBytes = new byte[64];
                System.arraycopy(keyBytes, 0, paddedBytes, 0, Math.min(keyBytes.length, 64));
                keyBytes = paddedBytes;
            }
        }
        signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generates a JWT access token from the given authentication object.
     *
     * @param authentication the authenticated user's authentication object
     * @param userId         the user's database ID
     * @return a signed JWT access token string
     */
    public String generateAccessToken(Authentication authentication, Long userId) {
        String username = authentication.getName();
        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        return generateAccessToken(username, userId, roles);
    }

    /**
     * Generates a JWT access token for the given user details.
     *
     * @param username the username to set as the subject
     * @param userId   the user's database ID
     * @param roles    the set of roles/authorities
     * @return a signed JWT access token string
     */
    public String generateAccessToken(String username, Long userId, Set<String> roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpirationMs);

        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(signingKey, Jwts.SIG.HS512)
                .compact();
    }

    /**
     * Extracts the username (subject) from a JWT token.
     *
     * @param token the JWT token string
     * @return the username embedded in the token
     */
    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Extracts the userId claim from a JWT token.
     *
     * @param token the JWT token string
     * @return the userId embedded in the token
     */
    public Long getUserIdFromToken(String token) {
        return parseClaims(token).get("userId", Long.class);
    }

    /**
     * Validates the given JWT token.
     *
     * @param token the JWT token string to validate
     * @return {@code true} if the token is valid and not expired
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (SecurityException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Malformed JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("Expired JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Returns the access token expiration time in milliseconds.
     *
     * @return access token TTL in milliseconds
     */
    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }

    /**
     * Returns the refresh token expiration time in milliseconds.
     *
     * @return refresh token TTL in milliseconds
     */
    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }

    /**
     * Parses and returns the claims from a JWT token.
     *
     * @param token the JWT token string
     * @return the parsed {@link Claims} object
     * @throws JwtException if the token is invalid
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
