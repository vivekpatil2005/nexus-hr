package com.nexushr.common.enums;

/**
 * Represents the employment status of an employee in the NexusHR system.
 */
public enum EmployeeStatus {

    /** Employee is currently active and working. */
    ACTIVE,

    /** Employee is currently on approved leave. */
    ON_LEAVE,

    /** Employee is in their probationary period. */
    PROBATION,

    /** Employee has submitted resignation and is serving notice. */
    NOTICE_PERIOD,

    /** Employee has been terminated by the organization. */
    TERMINATED,

    /** Employee has voluntarily resigned and completed their tenure. */
    RESIGNED
}
