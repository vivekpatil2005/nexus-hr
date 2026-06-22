package com.nexushr.common.event;

/**
 * Event published when a new user self-registers.
 * Used to trigger employee profile creation and linkage.
 */
public record UserRegisteredEvent(
        Long userId,
        String username,
        String email,
        String fullName
) {
}
