package com.nexushr.common.enums;

/**
 * Represents the processing status of a payroll run.
 */
public enum PayrollRunStatus {

    /** Payroll run has been created but calculations have not started. */
    DRAFT,

    /** Payroll calculations are currently being processed. */
    PROCESSING,

    /** Payroll run has been reviewed and approved for disbursement. */
    APPROVED,

    /** Payroll run is locked and disbursements have been initiated. */
    LOCKED,

    /** Payroll processing encountered errors and could not complete. */
    FAILED
}
