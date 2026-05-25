package com.nexushr.payroll.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SalaryStructureRequest(
    @NotNull(message = "Employee ID is required")
    Long employeeId,

    @NotNull(message = "Effective date is required")
    LocalDate effectiveFrom,

    @NotNull(message = "Basic salary is required")
    @Positive(message = "Basic salary must be positive")
    BigDecimal basic,

    BigDecimal hra,
    BigDecimal da,
    BigDecimal specialAllowance,
    BigDecimal conveyance,
    BigDecimal medical,
    BigDecimal lta,
    BigDecimal otherAllowances,

    @NotNull(message = "Gross salary is required")
    @Positive(message = "Gross salary must be positive")
    BigDecimal gross,

    @NotNull(message = "CTC is required")
    @Positive(message = "CTC must be positive")
    BigDecimal ctc
) {}
