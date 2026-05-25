package com.nexushr.attendance.dto;

import com.nexushr.common.enums.AttendanceStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record AttendanceResponse(
    Long id,
    Long employeeId,
    String employeeName,
    String empCode,
    LocalDate date,
    LocalDateTime checkIn,
    LocalDateTime checkOut,
    AttendanceStatus status,
    BigDecimal overtimeHours,
    String notes
) {}
