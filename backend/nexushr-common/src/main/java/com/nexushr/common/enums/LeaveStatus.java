package com.nexushr.common.enums;

/**
 * Represents the lifecycle status of a leave request.
 */
public enum LeaveStatus {

    /** Leave request has been created but not yet submitted. */
    DRAFT,

    /** Leave request has been submitted and is awaiting approval. */
    PENDING,

    /** Leave request has been approved by the authorizer. */
    APPROVED,

    /** Leave request has been rejected by the authorizer. */
    REJECTED,

    /** Leave request has been cancelled by the employee. */
    CANCELLED
}
