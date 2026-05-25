package com.nexushr.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a business rule is violated.
 * Results in an HTTP 422 Unprocessable Entity response.
 * <p>
 * Each violation can carry an optional rule code for programmatic handling
 * by API consumers.
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class BusinessRuleViolationException extends RuntimeException {

    private final String ruleCode;

    /**
     * Constructs the exception with a default rule code.
     *
     * @param message a human-readable description of the violation
     */
    public BusinessRuleViolationException(String message) {
        super(message);
        this.ruleCode = "BUSINESS_RULE_VIOLATION";
    }

    /**
     * Constructs the exception with a specific rule code.
     *
     * @param ruleCode a machine-readable code identifying the violated rule
     * @param message  a human-readable description of the violation
     */
    public BusinessRuleViolationException(String ruleCode, String message) {
        super(message);
        this.ruleCode = ruleCode;
    }

    public String getRuleCode() {
        return ruleCode;
    }
}
