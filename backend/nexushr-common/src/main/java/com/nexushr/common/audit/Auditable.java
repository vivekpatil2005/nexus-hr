package com.nexushr.common.audit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method for automatic audit logging.
 * When applied, the {@link AuditLogAspect} will record the action,
 * actor, entity type, and timestamp to the audit_logs table.
 *
 * <p>Usage example:
 * <pre>
 * {@code @Auditable(action = "CREATE", entity = "Employee")}
 * public Employee createEmployee(EmployeeRequest request) { ... }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditable {

    /**
     * The action being performed (e.g., "CREATE", "UPDATE", "DELETE", "APPROVE").
     */
    String action();

    /**
     * The entity type being acted upon (e.g., "Employee", "LeaveRequest").
     * If left empty, the aspect will attempt to infer it from the return type.
     */
    String entity() default "";
}
