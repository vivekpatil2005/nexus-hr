package com.nexushr.auth.controller;

import com.nexushr.auth.dto.*;
import com.nexushr.auth.entity.User;
import com.nexushr.auth.service.AuthService;
import com.nexushr.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for authentication operations.
 * <p>
 * Provides endpoints for user signup, login, token refresh, logout, and
 * retrieving the current authenticated user's profile.
 * All responses are wrapped in {@link ApiResponse} for consistent API structure.
 * </p>
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User registration, login, token management, and session operations")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Registers a new user account.
     *
     * @param request the signup request with user details
     * @return the authentication response with JWT tokens
     */
    @PostMapping("/signup")
    @Operation(summary = "Register a new user", description = "Creates a new user account and returns JWT tokens")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User registered successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input or duplicate username/email")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> signup(@Valid @RequestBody SignupRequest request) {
        log.info("Signup request received for username: {}", request.username());
        AuthResponse authResponse = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", authResponse));
    }

    /**
     * Authenticates a user with their credentials.
     *
     * @param request the login request with username/email and password
     * @return the authentication response with JWT tokens
     */
    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Validates credentials and returns JWT access and refresh tokens")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Authentication successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for: {}", request.usernameOrEmail());
        AuthResponse authResponse = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
    }

    /**
     * Refreshes an expired access token using a valid refresh token.
     * Implements token rotation — the old refresh token is revoked and a new one is issued.
     *
     * @param request the refresh token request
     * @return the authentication response with new JWT tokens
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Rotates the refresh token and issues a new access token")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid or expired refresh token")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse authResponse = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", authResponse));
    }

    /**
     * Logs out the currently authenticated user by revoking all their refresh tokens.
     *
     * @param authentication the current authentication context
     * @return a success response confirming logout
     */
    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Revokes all refresh tokens for the authenticated user")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Logged out successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<ApiResponse<Void>> logout(Authentication authentication) {
        authService.logout(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    /**
     * Returns the profile information of the currently authenticated user.
     *
     * @param userDetails the authenticated user's details from the security context
     * @return the current user's profile information
     */
    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Returns the profile of the currently authenticated user")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User profile retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = authService.getCurrentUser(userDetails.getUsername());

        Map<String, Object> userProfile = new java.util.HashMap<>();
        userProfile.put("id", user.getId());
        userProfile.put("username", user.getUsername());
        userProfile.put("email", user.getEmail());
        userProfile.put("fullName", user.getFullName());
        userProfile.put("roles", user.getRoles().stream().map(Enum::name).collect(Collectors.toSet()));
        userProfile.put("enabled", user.isEnabled());
        userProfile.put("createdAt", user.getCreatedAt());
        userProfile.put("employeeId", user.getEmployeeId());

        return ResponseEntity.ok(ApiResponse.success(userProfile));
    }

    /**
     * Changes the authenticated user's password.
     */
    @PostMapping("/change-password")
    @Operation(summary = "Change password", description = "Changes the currently logged-in user's password")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Password changed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid password input"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Password change request received for user: {}", userDetails.getUsername());
        authService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }

    /**
     * Retrieves a list of all users.
     */
    @GetMapping("/users")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'HR_MANAGER', 'MANAGER')")
    @Operation(summary = "Get all users", description = "Returns a list of all user profiles (for admin/HR/manager use)")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllUsers() {
        List<Map<String, Object>> users = authService.getAllUsers().stream().map(user -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", user.getId());
            map.put("username", user.getUsername());
            map.put("email", user.getEmail());
            map.put("fullName", user.getFullName());
            map.put("roles", user.getRoles().stream().map(Enum::name).collect(Collectors.toSet()));
            map.put("enabled", user.isEnabled());
            map.put("employeeId", user.getEmployeeId());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(users));
    }
}
