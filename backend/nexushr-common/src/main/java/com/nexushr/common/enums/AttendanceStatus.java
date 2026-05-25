package com.nexushr.common.enums;

/**
 * Represents the daily attendance status of an employee.
 */
public enum AttendanceStatus {
    /** Employee was present and checked in/out. */
    PRESENT,

    /** Employee was absent without approved leave. */
    ABSENT,

    /** Employee worked for a half day. */
    HALF_DAY,

    /** Employee was on approved leave. */
    ON_LEAVE,

    /** Gazetted/Company Holiday. */
    HOLIDAY,

    /** Standard weekend (Saturday/Sunday). */
    WEEKEND
}
