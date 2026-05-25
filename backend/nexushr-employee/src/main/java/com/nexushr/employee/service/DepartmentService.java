package com.nexushr.employee.service;

import com.nexushr.common.exception.DuplicateResourceException;
import com.nexushr.common.exception.ResourceNotFoundException;
import com.nexushr.employee.dto.DepartmentRequest;
import com.nexushr.employee.dto.DepartmentResponse;
import com.nexushr.employee.entity.Department;
import com.nexushr.employee.repository.DepartmentRepository;
import com.nexushr.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * Service for department CRUD with hierarchy support.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    @Transactional
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        if (departmentRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Department", "name", request.name());
        }

        Department department = Department.builder()
                .name(request.name())
                .description(request.description())
                .headId(request.headId())
                .build();

        if (request.parentId() != null) {
            Department parent = departmentRepository.findById(request.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.parentId()));
            department.setParent(parent);
        }

        department = departmentRepository.save(department);
        log.info("Created department: {}", department.getName());
        return toResponse(department);
    }

    @Transactional
    public DepartmentResponse updateDepartment(Long id, DepartmentRequest request) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));

        if (request.name() != null) department.setName(request.name());
        if (request.description() != null) department.setDescription(request.description());
        if (request.headId() != null) department.setHeadId(request.headId());

        if (request.parentId() != null) {
            Department parent = departmentRepository.findById(request.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.parentId()));
            department.setParent(parent);
        }

        department = departmentRepository.save(department);
        return toResponse(department);
    }

    @Transactional(readOnly = true)
    public DepartmentResponse getDepartment(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));
        return toResponse(department);
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAllDepartments() {
        return departmentRepository.findAllByActiveTrue().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Returns the full department tree starting from root departments.
     */
    @Transactional(readOnly = true)
    public List<DepartmentResponse> getDepartmentTree() {
        List<Department> roots = departmentRepository.findByParentIsNull();
        return roots.stream()
                .map(this::toTreeResponse)
                .toList();
    }

    @Transactional
    public void deleteDepartment(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));
        department.setActive(false);
        departmentRepository.save(department);
        log.info("Deactivated department: {}", department.getName());
    }

    // ─── Helpers ───

    private DepartmentResponse toResponse(Department d) {
        return DepartmentResponse.builder()
                .id(d.getId())
                .name(d.getName())
                .description(d.getDescription())
                .parentId(d.getParent() != null ? d.getParent().getId() : null)
                .parentName(d.getParent() != null ? d.getParent().getName() : null)
                .headId(d.getHeadId())
                .active(d.isActive())
                .employeeCount(employeeRepository.countByDepartmentId(d.getId()))
                .children(Collections.emptyList())
                .build();
    }

    private DepartmentResponse toTreeResponse(Department d) {
        List<DepartmentResponse> children = d.getChildren().stream()
                .filter(Department::isActive)
                .map(this::toTreeResponse)
                .toList();

        return DepartmentResponse.builder()
                .id(d.getId())
                .name(d.getName())
                .description(d.getDescription())
                .parentId(d.getParent() != null ? d.getParent().getId() : null)
                .headId(d.getHeadId())
                .active(d.isActive())
                .employeeCount(employeeRepository.countByDepartmentId(d.getId()))
                .children(children)
                .build();
    }
}
