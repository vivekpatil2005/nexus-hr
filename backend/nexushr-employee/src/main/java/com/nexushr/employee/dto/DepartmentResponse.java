package com.nexushr.employee.dto;

import lombok.Builder;

import java.util.List;

/**
 * Department response DTO with optional child hierarchy.
 */
@Builder
public record DepartmentResponse(
        Long id,
        String name,
        String description,
        Long parentId,
        String parentName,
        Long headId,
        String headName,
        boolean active,
        long employeeCount,
        List<DepartmentResponse> children
) {}
