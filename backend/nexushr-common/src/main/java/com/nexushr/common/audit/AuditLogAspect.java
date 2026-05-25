package com.nexushr.common.audit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * AOP aspect that intercepts methods annotated with {@link Auditable}
 * and records audit log entries capturing the actor, action, entity,
 * and client IP address.
 *
 * <p>Audit logging failures are caught and logged as warnings so they
 * never disrupt the underlying business logic.
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogRepository auditLogRepository;

    /**
     * Around advice that captures audit information before and after
     * the annotated method executes.
     *
     * @param joinPoint the join point representing the intercepted method
     * @param auditable the annotation instance with action and entity metadata
     * @return the result of the intercepted method
     * @throws Throwable if the intercepted method throws an exception
     */
    @Around("@annotation(auditable)")
    public Object auditMethod(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        Object result = joinPoint.proceed();

        try {
            String actor = resolveActor();
            String entityType = resolveEntityType(auditable, joinPoint);
            String entityId = resolveEntityId(result);
            String ipAddress = resolveIpAddress();
            String details = buildDetails(joinPoint);

            AuditLog auditLog = AuditLog.builder()
                    .actor(actor)
                    .action(auditable.action())
                    .entityType(entityType)
                    .entityId(entityId)
                    .details(details)
                    .ipAddress(ipAddress)
                    .timestamp(LocalDateTime.now())
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log recorded: {} performed {} on {} [{}]",
                    actor, auditable.action(), entityType, entityId);
        } catch (Exception ex) {
            log.warn("Failed to record audit log for action '{}': {}",
                    auditable.action(), ex.getMessage(), ex);
        }

        return result;
    }

    /**
     * Resolves the current actor (username) from the Spring Security context.
     */
    private String resolveActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "SYSTEM";
    }

    /**
     * Resolves the entity type from the annotation or infers it from the method return type.
     */
    private String resolveEntityType(Auditable auditable, ProceedingJoinPoint joinPoint) {
        if (!auditable.entity().isEmpty()) {
            return auditable.entity();
        }
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getReturnType().getSimpleName();
    }

    /**
     * Attempts to extract an entity ID from the method result via reflection.
     * Looks for a {@code getId()} method on the result object.
     */
    private String resolveEntityId(Object result) {
        if (result == null) {
            return null;
        }
        try {
            var method = result.getClass().getMethod("getId");
            Object id = method.invoke(result);
            return id != null ? id.toString() : null;
        } catch (NoSuchMethodException e) {
            // Result object doesn't have getId() — this is fine
            return null;
        } catch (Exception e) {
            log.debug("Could not extract entity ID from result: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Resolves the client IP address from the current HTTP request.
     * Checks the {@code X-Forwarded-For} header first for proxied requests.
     */
    private String resolveIpAddress() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String forwardedFor = request.getHeader("X-Forwarded-For");
                if (forwardedFor != null && !forwardedFor.isEmpty()) {
                    return forwardedFor.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.debug("Could not resolve IP address: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Builds a brief detail string from the method signature and arguments.
     */
    private String buildDetails(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return String.format("%s.%s",
                signature.getDeclaringType().getSimpleName(),
                signature.getName());
    }
}
