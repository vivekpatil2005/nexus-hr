package com.nexushr.payroll.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SalaryStructureResponse(
    Long id,
    Long employeeId,
    String employeeName,
    LocalDate effectiveFrom,
    LocalDate effectiveTo,
    BigDecimal basic,
    BigDecimal hra,
    BigDecimal da,
    BigDecimal specialAllowance,
    BigDecimal conveyance,
    BigDecimal medical,
    BigDecimal lta,
    BigDecimal otherAllowances,
    BigDecimal gross,
    BigDecimal ctc,
    boolean active
) {}
