package com.nexushr.common.exception;

import com.nexushr.common.dto.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for the NexusHR platform.
 * Intercepts all controller-level exceptions and returns consistent
 * {@link ApiResponse} error payloads with appropriate HTTP status codes.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles Spring Security authentication exceptions (e.g. BadCredentialsException).
     *
     * @param ex the exception
     * @return 401 response with error details
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Object>> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Authentication failed: " + ex.getMessage()));
    }

    /**
     * Handles illegal argument and state exceptions.
     *
     * @param ex the exception
     * @return 400 response with error details
     */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(RuntimeException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handles resource not found exceptions.
     *
     * @param ex the exception
     * @return 404 response with error details
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handles business rule violation exceptions.
     *
     * @param ex the exception
     * @return 422 response with error details and rule code
     */
    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessRuleViolation(BusinessRuleViolationException ex) {
        log.warn("Business rule violation [{}]: {}", ex.getRuleCode(), ex.getMessage());
        Map<String, String> details = new HashMap<>();
        details.put("ruleCode", ex.getRuleCode());
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.<Object>builder()
                        .success(false)
                        .message(ex.getMessage())
                        .data(details)
                        .build());
    }

    /**
     * Handles unauthorized access exceptions.
     *
     * @param ex the exception
     * @return 401 response with error details
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnauthorized(UnauthorizedException ex) {
        log.warn("Unauthorized access: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handles duplicate resource exceptions.
     *
     * @param ex the exception
     * @return 409 response with error details
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Object>> handleDuplicateResource(DuplicateResourceException ex) {
        log.warn("Duplicate resource: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handles Bean Validation errors from {@code @Valid} annotated request bodies.
     *
     * @param ex the exception containing field-level validation errors
     * @return 400 response with a map of field names to error messages
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        log.warn("Validation failed: {} field error(s)", ex.getBindingResult().getErrorCount());
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value",
                        (existing, replacement) -> existing
                ));
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Object>builder()
                        .success(false)
                        .message("Validation failed")
                        .data(fieldErrors)
                        .build());
    }

    /**
     * Handles constraint violation exceptions from path variables and request parameters.
     *
     * @param ex the exception containing constraint violations
     * @return 400 response with a map of property paths to violation messages
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("Constraint violation: {}", ex.getMessage());
        Map<String, String> violations = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage,
                        (existing, replacement) -> existing
                ));
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Object>builder()
                        .success(false)
                        .message("Constraint violation")
                        .data(violations)
                        .build());
    }

    /**
     * Handles Spring Security access denied exceptions.
     *
     * @param ex the exception
     * @return 403 response with error details
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Access denied: you do not have permission to perform this action"));
    }

    /**
     * Catch-all handler for any unhandled exceptions.
     *
     * @param ex the exception
     * @return 500 response with a generic error message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception ex) {
        String className = ex.getClass().getName();
        if (className.contains("security.authentication") || 
            className.contains("security.core.AuthenticationException") || 
            className.contains("BadCredentialsException") || 
            className.contains("UsernameNotFoundException")) {
            log.warn("Authentication failed (fallback handler): {}", ex.getMessage());
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication failed: " + ex.getMessage()));
        }

        log.error("Unhandled exception occurred", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred. Please try again later."));
    }
}
