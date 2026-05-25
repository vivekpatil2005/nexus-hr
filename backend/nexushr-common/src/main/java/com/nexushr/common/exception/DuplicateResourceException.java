package com.nexushr.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when attempting to create a resource that already exists.
 * Results in an HTTP 409 Conflict response.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateResourceException extends RuntimeException {

    /**
     * Constructs a new DuplicateResourceException.
     *
     * @param resourceName the name of the resource (e.g., "Employee", "Department")
     * @param fieldName    the field that caused the conflict (e.g., "email")
     * @param fieldValue   the duplicate value
     */
    public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s already exists with %s: '%s'", resourceName, fieldName, fieldValue));
    }
}
