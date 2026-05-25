package com.nexushr.employee.dto;

import com.nexushr.common.enums.EmployeeStatus;
import lombok.Builder;

/**
 * Lightweight employee summary for list views.
 */
@Builder
public record EmployeeSummaryResponse(
        Long id,
        String empCode,
        String fullName,
        String email,
        String departmentName,
        String designation,
        EmployeeStatus status,
        String profilePhotoUrl
) {}
