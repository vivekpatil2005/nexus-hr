package com.nexushr.common.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JPA repository for {@link AuditLog} entities.
 * Provides standard CRUD operations plus custom query methods
 * for audit trail retrieval.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Finds all audit log entries for a specific actor, ordered by timestamp descending.
     *
     * @param actor the username of the actor
     * @return list of audit log entries
     */
    List<AuditLog> findByActorOrderByTimestampDesc(String actor);

    /**
     * Finds all audit log entries for a specific entity type and entity ID.
     *
     * @param entityType the type of entity (e.g., "Employee")
     * @param entityId   the identifier of the entity
     * @return list of audit log entries
     */
    List<AuditLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(String entityType, String entityId);

    /**
     * Finds all audit log entries within a given time range.
     *
     * @param start the start of the time range (inclusive)
     * @param end   the end of the time range (inclusive)
     * @return list of audit log entries
     */
    List<AuditLog> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime start, LocalDateTime end);
}
