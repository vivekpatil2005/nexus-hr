package com.nexushr.common.enums;

/**
 * Represents the roles available in the NexusHR RBAC system.
 * Used in conjunction with Spring Security's {@code @PreAuthorize}
 * for method-level authorization.
 */
public enum Role {

    /** Full system administrator with unrestricted access. */
    ADMIN,

    /** HR department manager with access to HR-specific operations. */
    HR_MANAGER,

    /** Team/department manager with access to their direct reports' data. */
    MANAGER,

    /** Standard employee with access to their own data only. */
    EMPLOYEE
}
