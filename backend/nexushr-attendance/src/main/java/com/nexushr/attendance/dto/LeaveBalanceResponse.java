package com.nexushr.attendance.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record LeaveBalanceResponse(
    Long id,
    Long leaveTypeId,
    String leaveTypeName,
    Integer year,
    BigDecimal totalDays,
    BigDecimal usedDays,
    BigDecimal pendingDays
) {}
