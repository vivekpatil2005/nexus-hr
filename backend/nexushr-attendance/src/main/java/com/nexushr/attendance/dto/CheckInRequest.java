package com.nexushr.attendance.dto;

import jakarta.validation.constraints.Size;

public record CheckInRequest(
    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    String notes
) {}
