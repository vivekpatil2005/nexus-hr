package com.nexushr.employee.dto;

import com.nexushr.common.enums.EmployeeStatus;
import com.nexushr.employee.entity.Gender;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Full employee response DTO.
 */
@Builder
public record EmployeeResponse(
        Long id,
        String empCode,
        String firstName,
        String lastName,
        String fullName,
        String email,
        String phone,
        Long departmentId,
        String departmentName,
        Long managerId,
        String managerName,
        String designation,
        EmployeeStatus status,
        LocalDate hireDate,
        LocalDate exitDate,
        Map<String, Object> skills,
        BigDecimal ctc,
        String profilePhotoUrl,
        String address,
        String city,
        String state,
        LocalDate dateOfBirth,
        Gender gender,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
