package com.nexushr.employee.dto;

import com.nexushr.common.enums.EmployeeStatus;
import com.nexushr.employee.entity.Gender;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * DTO for creating or updating an employee.
 */
public record EmployeeRequest(
        @NotBlank(message = "First name is required")
        @Size(max = 50)
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 50)
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @Size(max = 15)
        String phone,

        Long departmentId,

        Long managerId,

        @Size(max = 100)
        String designation,

        EmployeeStatus status,

        @NotNull(message = "Hire date is required")
        LocalDate hireDate,

        LocalDate exitDate,

        Map<String, Object> skills,

        @DecimalMin(value = "0.0")
        BigDecimal ctc,

        String profilePhotoUrl,

        @Size(max = 200)
        String address,

        @Size(max = 100)
        String city,

        @Size(max = 50)
        String state,

        LocalDate dateOfBirth,

        Gender gender
) {}
