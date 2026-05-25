package com.nexushr.employee.dto;

import lombok.Builder;

import java.util.List;

/**
 * Tree node for org-chart visualization.
 */
@Builder
public record OrgChartNode(
        Long id,
        String empCode,
        String name,
        String designation,
        String departmentName,
        String profilePhotoUrl,
        List<OrgChartNode> children
) {}
