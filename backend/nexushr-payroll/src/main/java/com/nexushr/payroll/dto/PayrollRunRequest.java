package com.nexushr.payroll.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PayrollRunRequest(
    @NotNull(message = "Month is required")
    @Min(1) @Max(12)
    Integer periodMonth,

    @NotNull(message = "Year is required")
    @Min(2020)
    Integer periodYear,

    String notes
) {}
