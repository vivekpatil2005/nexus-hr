package com.nexushr.payroll.dto;

import com.nexushr.common.enums.PayrollRunStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PayrollRunResponse(
    Long id,
    int periodMonth,
    int periodYear,
    PayrollRunStatus status,
    BigDecimal totalGross,
    BigDecimal totalDeductions,
    BigDecimal totalNet,
    int employeeCount,
    String runByUsername,
    String approvedByUsername,
    String notes,
    LocalDateTime startedAt,
    LocalDateTime completedAt,
    LocalDateTime createdAt
) {}
