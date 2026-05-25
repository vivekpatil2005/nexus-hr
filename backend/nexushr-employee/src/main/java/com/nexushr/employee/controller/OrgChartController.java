package com.nexushr.employee.controller;

import com.nexushr.common.dto.ApiResponse;
import com.nexushr.employee.dto.OrgChartNode;
import com.nexushr.employee.service.OrgChartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for org-chart visualization data.
 */
@RestController
@RequestMapping("/api/org-chart")
@Tag(name = "Org Chart", description = "Organizational hierarchy visualization")
@RequiredArgsConstructor
public class OrgChartController {

    private final OrgChartService orgChartService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get full org chart")
    public ResponseEntity<ApiResponse<List<OrgChartNode>>> getFullOrgChart() {
        List<OrgChartNode> orgChart = orgChartService.buildFullOrgChart();
        return ResponseEntity.ok(ApiResponse.success(orgChart));
    }

    @GetMapping("/{employeeId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get org chart subtree for an employee")
    public ResponseEntity<ApiResponse<OrgChartNode>> getSubTree(@PathVariable Long employeeId) {
        OrgChartNode subTree = orgChartService.getSubTree(employeeId);
        return ResponseEntity.ok(ApiResponse.success(subTree));
    }
}
