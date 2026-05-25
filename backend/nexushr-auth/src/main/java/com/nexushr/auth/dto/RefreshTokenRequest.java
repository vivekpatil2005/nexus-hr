package com.nexushr.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for refreshing an expired access token using a valid refresh token.
 *
 * @param refreshToken the refresh token obtained during login or a previous refresh
 */
public record RefreshTokenRequest(
        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {
}
