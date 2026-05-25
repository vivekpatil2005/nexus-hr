package com.nexushr.attendance.dto;

import com.nexushr.common.enums.LeaveStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record LeaveResponse(
    Long id,
    Long employeeId,
    String employeeName,
    Long leaveTypeId,
    String leaveTypeName,
    LocalDate fromDate,
    LocalDate toDate,
    BigDecimal totalDays,
    LeaveStatus status,
    Long approverId,
    String approverName,
    String reason,
    String rejectionReason,
    LocalDateTime approvedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
