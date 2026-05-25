package com.nexushr.attendance.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LeaveApprovalRequest(
    @NotNull(message = "Approved status is required")
    Boolean approved,
    
    @Size(max = 1000, message = "Rejection reason cannot exceed 1000 characters")
    String rejectionReason
) {}
