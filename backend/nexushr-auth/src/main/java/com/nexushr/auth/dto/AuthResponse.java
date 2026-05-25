package com.nexushr.auth.dto;

import java.util.Set;

/**
 * Response DTO returned after successful authentication or token refresh.
 *
 * @param accessToken  the JWT access token
 * @param refreshToken the refresh token for obtaining new access tokens
 * @param tokenType    the token type, always "Bearer"
 * @param expiresIn    the access token expiry duration in seconds
 * @param username     the authenticated user's username
 * @param roles        the set of roles assigned to the user
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        String username,
        Set<String> roles
) {
    /**
     * Creates an AuthResponse with the default "Bearer" token type.
     *
     * @param accessToken  the JWT access token
     * @param refreshToken the refresh token
     * @param expiresIn    token expiry in seconds
     * @param username     the authenticated username
     * @param roles        the user's roles
     * @return a new {@link AuthResponse} instance
     */
    public static AuthResponse of(String accessToken, String refreshToken,
                                   long expiresIn, String username, Set<String> roles) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresIn, username, roles);
    }
}
