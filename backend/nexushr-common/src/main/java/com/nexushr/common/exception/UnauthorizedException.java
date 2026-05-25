package com.nexushr.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a request lacks valid authentication credentials.
 * Results in an HTTP 401 Unauthorized response.
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class UnauthorizedException extends RuntimeException {

    /**
     * Constructs a new UnauthorizedException.
     *
     * @param message a human-readable description of the authentication failure
     */
    public UnauthorizedException(String message) {
        super(message);
    }
}
