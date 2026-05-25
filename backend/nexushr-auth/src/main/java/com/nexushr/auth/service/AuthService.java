package com.nexushr.auth.service;

import com.nexushr.auth.dto.AuthResponse;
import com.nexushr.auth.dto.LoginRequest;
import com.nexushr.auth.dto.RefreshTokenRequest;
import com.nexushr.auth.dto.SignupRequest;
import com.nexushr.auth.dto.ChangePasswordRequest;
import com.nexushr.auth.entity.RefreshToken;
import com.nexushr.auth.entity.User;
import com.nexushr.auth.repository.RefreshTokenRepository;
import com.nexushr.auth.repository.UserRepository;
import com.nexushr.auth.security.JwtTokenProvider;
import com.nexushr.common.enums.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Core authentication service handling user registration, login, token refresh, and logout.
 * <p>
 * Implements secure refresh token rotation with family-based reuse detection.
 * When a revoked refresh token is presented (indicating potential token theft),
 * the entire token family is revoked as a security countermeasure.
 * </p>
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Registers a new user account.
     * <p>
     * Validates uniqueness of username and email, encodes the password, assigns roles,
     * and returns JWT tokens upon successful registration.
     * </p>
     *
     * @param request the signup request containing user details
     * @return an {@link AuthResponse} with the generated tokens
     * @throws IllegalArgumentException if the username or email is already taken
     */
    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username '" + request.username() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email '" + request.email() + "' is already registered");
        }

        // Resolve roles from request; default to EMPLOYEE if none specified
        Set<Role> roles = resolveRoles(request.roles());

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .roles(roles)
                .enabled(true)
                .build();

        user = userRepository.save(user);
        log.info("New user registered: username={}, roles={}", user.getUsername(), roles);

        // Generate tokens
        Set<String> roleNames = roles.stream().map(Enum::name).collect(Collectors.toSet());
        String accessToken = jwtTokenProvider.generateAccessToken(user.getUsername(), user.getId(), roleNames);
        String refreshToken = createRefreshToken(user.getUsername());

        return AuthResponse.of(
                accessToken,
                refreshToken,
                jwtTokenProvider.getAccessTokenExpirationMs() / 1000,
                user.getUsername(),
                roleNames
        );
    }

    /**
     * Authenticates a user and returns JWT tokens.
     *
     * @param request the login request with credentials
     * @return an {@link AuthResponse} with the generated tokens
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.usernameOrEmail(),
                        request.password()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userRepository.findByUsername(authentication.getName())
                .or(() -> userRepository.findByEmail(authentication.getName()))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Set<String> roleNames = user.getRoles().stream()
                .map(Enum::name)
                .collect(Collectors.toSet());

        String accessToken = jwtTokenProvider.generateAccessToken(authentication, user.getId());
        String refreshToken = createRefreshToken(user.getUsername());

        log.info("User logged in: username={}", user.getUsername());

        return AuthResponse.of(
                accessToken,
                refreshToken,
                jwtTokenProvider.getAccessTokenExpirationMs() / 1000,
                user.getUsername(),
                roleNames
        );
    }

    /**
     * Refreshes an access token using a valid refresh token.
     * <p>
     * Implements token rotation: the presented refresh token is revoked and a new one
     * is created in the same family. If the presented token was already revoked
     * (indicating potential reuse/theft), the entire family is revoked.
     * </p>
     *
     * @param request the refresh token request
     * @return a new {@link AuthResponse} with rotated tokens
     * @throws IllegalArgumentException if the token is invalid, expired, or reuse is detected
     */
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        // Reuse detection: if the token is already revoked, this is a potential theft
        if (storedToken.isRevoked()) {
            log.warn("Refresh token reuse detected for family '{}', user '{}'. Revoking entire family.",
                    storedToken.getFamily(), storedToken.getUsername());
            refreshTokenRepository.revokeAllByFamily(storedToken.getFamily());
            throw new IllegalArgumentException(
                    "Refresh token reuse detected. All sessions in this family have been revoked for security.");
        }

        // Check expiry
        if (storedToken.isExpired()) {
            storedToken.setRevoked(true);
            refreshTokenRepository.save(storedToken);
            throw new IllegalArgumentException("Refresh token has expired. Please log in again.");
        }

        // Revoke the current token (rotation)
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        // Create new refresh token in the same family
        String newRefreshToken = createRefreshTokenInFamily(storedToken.getUsername(), storedToken.getFamily());

        // Generate new access token
        User user = userRepository.findByUsername(storedToken.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Set<String> roleNames = user.getRoles().stream()
                .map(Enum::name)
                .collect(Collectors.toSet());

        String accessToken = jwtTokenProvider.generateAccessToken(user.getUsername(), user.getId(), roleNames);

        log.debug("Token refreshed for user '{}'", user.getUsername());

        return AuthResponse.of(
                accessToken,
                newRefreshToken,
                jwtTokenProvider.getAccessTokenExpirationMs() / 1000,
                user.getUsername(),
                roleNames
        );
    }

    /**
     * Logs out a user by revoking all their refresh tokens.
     *
     * @param username the username of the user to log out
     */
    @Transactional
    public void logout(String username) {
        refreshTokenRepository.deleteByUsername(username);
        SecurityContextHolder.clearContext();
        log.info("User logged out: username={}", username);
    }

    /**
     * Retrieves the currently authenticated user's details.
     *
     * @param username the authenticated username
     * @return the {@link User} entity
     * @throws IllegalArgumentException if the user is not found
     */
    @Transactional(readOnly = true)
    public User getCurrentUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    /**
     * Changes the password of an authenticated user.
     *
     * @param username the username of the user changing the password
     * @param request  the request DTO containing current and new passwords
     */
    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Incorrect current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        log.info("Password changed successfully for user: {}", username);
    }

    /**
     * Retrieves all users.
     */
    @Transactional(readOnly = true)
    public java.util.List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // ─── Private Helpers ──────────────────────────────────────────────────

    /**
     * Creates a new refresh token with a new family (for login/signup).
     */
    private String createRefreshToken(String username) {
        String family = UUID.randomUUID().toString();
        return createRefreshTokenInFamily(username, family);
    }

    /**
     * Creates a new refresh token in an existing family (for token rotation).
     */
    private String createRefreshTokenInFamily(String username, String family) {
        String tokenValue = UUID.randomUUID().toString();

        RefreshToken refreshToken = RefreshToken.builder()
                .token(tokenValue)
                .username(username)
                .expiryDate(Instant.now().plusMillis(jwtTokenProvider.getRefreshTokenExpirationMs()))
                .revoked(false)
                .family(family)
                .build();

        refreshTokenRepository.save(refreshToken);
        return tokenValue;
    }

    /**
     * Resolves role names from the signup request to {@link Role} enum values.
     * Defaults to {@link Role#EMPLOYEE} if no roles are specified.
     */
    private Set<Role> resolveRoles(Set<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return Set.of(Role.EMPLOYEE);
        }

        return roleNames.stream()
                .map(name -> {
                    try {
                        return Role.valueOf(name.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Invalid role: " + name);
                    }
                })
                .collect(Collectors.toSet());
    }
}
