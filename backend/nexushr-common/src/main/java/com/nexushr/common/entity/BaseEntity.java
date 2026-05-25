package com.nexushr.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Abstract base entity providing common fields for all NexusHR JPA entities.
 * Includes auto-generated ID and JPA auditing timestamps.
 *
 * <p>Subclasses automatically inherit:
 * <ul>
 *   <li>{@code id} — auto-generated primary key</li>
 *   <li>{@code createdAt} — set once on initial persist</li>
 *   <li>{@code updatedAt} — updated on every modification</li>
 * </ul>
 *
 * <p>Requires {@code @EnableJpaAuditing} on a configuration class.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Data
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
