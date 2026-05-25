package com.nexushr.common.event;

/**
 * Event published when a new employee is registered.
 * Used to decouple employee creation from user account generation and email notifications.
 */
public record EmployeeCreatedEvent(
        Long employeeId,
        String firstName,
        String lastName,
        String email,
        String designation
) {
}
