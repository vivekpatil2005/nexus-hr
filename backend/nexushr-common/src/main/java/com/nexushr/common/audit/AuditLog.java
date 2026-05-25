package com.nexushr.common.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * JPA entity representing an audit log entry.
 * Each entry captures who performed what action on which entity and when.
 */
@Entity
@Table(name = "audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The username or identifier of the user who performed the action. */
    @Column(nullable = false)
    private String actor;

    /** The action that was performed (e.g., CREATE, UPDATE, DELETE). */
    @Column(nullable = false)
    private String action;

    /** The type of entity the action was performed on (e.g., Employee, LeaveRequest). */
    @Column
    private String entityType;

    /** The identifier of the specific entity instance. */
    @Column
    private String entityId;

    /** Additional details about the action, stored as free-form text. */
    @Column(columnDefinition = "TEXT")
    private String details;

    /** The IP address of the client that initiated the action. */
    @Column
    private String ipAddress;

    /** The timestamp when the action was recorded. */
    @Column(nullable = false)
    private LocalDateTime timestamp;

    /**
     * Sets the timestamp to the current time if not already set before persisting.
     */
    @PrePersist
    public void prePersist() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
