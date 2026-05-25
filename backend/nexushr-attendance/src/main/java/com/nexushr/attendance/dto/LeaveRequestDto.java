package com.nexushr.attendance.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record LeaveRequestDto(
    @NotNull(message = "Leave type ID is required")
    Long leaveTypeId,
    
    @NotNull(message = "From date is required")
    LocalDate fromDate,
    
    @NotNull(message = "To date is required")
    LocalDate toDate,
    
    @Size(max = 1000, message = "Reason cannot exceed 1000 characters")
    String reason
) {}
