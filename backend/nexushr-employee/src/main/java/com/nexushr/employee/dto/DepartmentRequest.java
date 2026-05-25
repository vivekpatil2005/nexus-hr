package com.nexushr.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for creating or updating a department.
 */
public record DepartmentRequest(
        @NotBlank(message = "Department name is required")
        @Size(max = 100)
        String name,

        @Size(max = 500)
        String description,

        Long parentId,

        Long headId
) {}
