package com.nexushr.employee.service;

import com.nexushr.employee.dto.OrgChartNode;
import com.nexushr.employee.entity.Employee;
import com.nexushr.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for building hierarchical org-chart data from employee manager relationships.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrgChartService {

    private final EmployeeRepository employeeRepository;

    /**
     * Builds the full org chart starting from the root (employees with no manager).
     */
    @Transactional(readOnly = true)
    public List<OrgChartNode> buildFullOrgChart() {
        List<Employee> roots = employeeRepository.findAllByManagerId(null);
        if (roots.isEmpty()) {
            // Fallback: find employees without managers
            roots = employeeRepository.findAll().stream()
                    .filter(e -> e.getManager() == null)
                    .toList();
        }
        return roots.stream()
                .map(this::buildSubTree)
                .toList();
    }

    /**
     * Builds the org chart subtree for a specific employee.
     */
    @Transactional(readOnly = true)
    public OrgChartNode getSubTree(Long employeeId) {
        Employee root = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new com.nexushr.common.exception.ResourceNotFoundException(
                        "Employee", "id", employeeId));
        return buildSubTree(root);
    }

    private OrgChartNode buildSubTree(Employee employee) {
        List<Employee> directReports = employeeRepository.findAllByManagerId(employee.getId());
        List<OrgChartNode> children = directReports.stream()
                .map(this::buildSubTree)
                .toList();

        return OrgChartNode.builder()
                .id(employee.getId())
                .empCode(employee.getEmpCode())
                .name(employee.getFullName())
                .designation(employee.getDesignation())
                .departmentName(employee.getDepartment() != null ? employee.getDepartment().getName() : null)
                .profilePhotoUrl(employee.getProfilePhotoUrl())
                .children(children)
                .build();
    }
}
