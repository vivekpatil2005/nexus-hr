package com.nexushr.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for user authentication.
 * Accepts either a username or email as the login identifier.
 *
 * @param usernameOrEmail the username or email address used for login
 * @param password        the user's plaintext password
 */
public record LoginRequest(
        @NotBlank(message = "Username or email is required")
        String usernameOrEmail,

        @NotBlank(message = "Password is required")
        String password
) {
}
