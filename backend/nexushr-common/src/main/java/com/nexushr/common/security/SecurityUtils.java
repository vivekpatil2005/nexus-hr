package com.nexushr.common.security;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Utility class for extracting security-related information from the
 * current Spring Security context.
 *
 * <p>All methods are static and operate on the thread-local
 * {@link SecurityContextHolder}.
 */
@Slf4j
@UtilityClass
public class SecurityUtils {

    private static final String ROLE_PREFIX = "ROLE_";

    /**
     * Returns the username of the currently authenticated user.
     *
     * @return an {@link Optional} containing the username, or empty if unauthenticated
     */
    public static Optional<String> getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.ofNullable(authentication.getName());
        }
        return Optional.empty();
    }

    /**
     * Returns the username of the currently authenticated user, or "system" if not authenticated.
     *
     * @return the current username or "system"
     */
    public static String getCurrentUsernameOrSystem() {
        return getCurrentUsername().orElse("system");
    }

    /**
     * Checks whether the currently authenticated user has the specified role.
     * The role name should not include the {@code ROLE_} prefix.
     *
     * @param role the role name to check (e.g., "ADMIN", "MANAGER")
     * @return {@code true} if the user has the specified role
     */
    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        String roleWithPrefix = role.startsWith(ROLE_PREFIX) ? role : ROLE_PREFIX + role;
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals(roleWithPrefix));
    }

    /**
     * Checks whether the currently authenticated user has the ADMIN role.
     *
     * @return {@code true} if the user is an administrator
     */
    public static boolean isAdmin() {
        return hasRole("ADMIN");
    }

    /**
     * Checks whether the currently authenticated user has the HR_MANAGER role.
     *
     * @return {@code true} if the user is an HR manager
     */
    public static boolean isHrManager() {
        return hasRole("HR_MANAGER");
    }

    /**
     * Checks whether the currently authenticated user has the MANAGER role.
     *
     * @return {@code true} if the user is a manager
     */
    public static boolean isManager() {
        return hasRole("MANAGER");
    }
}
