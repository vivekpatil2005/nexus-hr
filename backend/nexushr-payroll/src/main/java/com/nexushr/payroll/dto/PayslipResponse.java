package com.nexushr.payroll.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record PayslipResponse(
    Long id,
    Long payrollRunId,
    int periodMonth,
    int periodYear,
    Long employeeId,
    String empCode,
    String employeeName,
    String department,
    String designation,
    BigDecimal basic,
    BigDecimal hra,
    BigDecimal da,
    BigDecimal specialAllowance,
    BigDecimal otherEarnings,
    BigDecimal gross,
    BigDecimal pfEmployee,
    BigDecimal pfEmployer,
    BigDecimal esiEmployee,
    BigDecimal esiEmployer,
    BigDecimal professionalTax,
    BigDecimal tds,
    BigDecimal otherDeductions,
    BigDecimal totalDeductions,
    BigDecimal netSalary,
    Map<String, Object> deductionsJson,
    String pdfS3Key,
    Integer workingDays,
    Integer presentDays,
    BigDecimal lossOfPayDays,
    LocalDateTime createdAt
) {}
