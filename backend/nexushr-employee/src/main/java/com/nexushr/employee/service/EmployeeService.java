package com.nexushr.employee.service;

import com.nexushr.common.dto.PagedResponse;
import com.nexushr.common.enums.EmployeeStatus;
import com.nexushr.common.exception.DuplicateResourceException;
import com.nexushr.common.exception.ResourceNotFoundException;
import com.nexushr.employee.dto.EmployeeRequest;
import com.nexushr.employee.dto.EmployeeResponse;
import com.nexushr.employee.dto.EmployeeSummaryResponse;
import com.nexushr.employee.entity.Department;
import com.nexushr.employee.entity.Employee;
import com.nexushr.employee.repository.DepartmentRepository;
import com.nexushr.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import com.nexushr.common.event.EmployeeCreatedEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for employee lifecycle management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Creates a new employee with an auto-generated emp code (NEX-0001 format).
     */
    @Transactional
    public EmployeeResponse createEmployee(EmployeeRequest request) {
        if (employeeRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Employee", "email", request.email());
        }

        Employee employee = Employee.builder()
                .empCode(generateEmpCode())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .phone(request.phone())
                .designation(request.designation())
                .status(request.status() != null ? request.status() : EmployeeStatus.ACTIVE)
                .hireDate(request.hireDate())
                .exitDate(request.exitDate())
                .skills(request.skills())
                .ctc(request.ctc())
                .profilePhotoUrl(request.profilePhotoUrl())
                .address(request.address())
                .city(request.city())
                .state(request.state())
                .dateOfBirth(request.dateOfBirth())
                .gender(request.gender())
                .build();

        if (request.departmentId() != null) {
            Department dept = departmentRepository.findById(request.departmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.departmentId()));
            employee.setDepartment(dept);
        }

        if (request.managerId() != null) {
            Employee manager = employeeRepository.findById(request.managerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager", "id", request.managerId()));
            employee.setManager(manager);
        }

        employee = employeeRepository.save(employee);
        log.info("Created employee: {} ({})", employee.getFullName(), employee.getEmpCode());

        eventPublisher.publishEvent(new EmployeeCreatedEvent(
                employee.getId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getEmail(),
                employee.getDesignation()
        ));

        return toResponse(employee);
    }

    /**
     * Updates an existing employee (partial update).
     */
    @Transactional
    public EmployeeResponse updateEmployee(Long id, EmployeeRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));

        if (request.firstName() != null) employee.setFirstName(request.firstName());
        if (request.lastName() != null) employee.setLastName(request.lastName());
        if (request.email() != null && !request.email().equals(employee.getEmail())) {
            if (employeeRepository.existsByEmail(request.email())) {
                throw new DuplicateResourceException("Employee", "email", request.email());
            }
            employee.setEmail(request.email());
        }
        if (request.phone() != null) employee.setPhone(request.phone());
        if (request.designation() != null) employee.setDesignation(request.designation());
        if (request.status() != null) employee.setStatus(request.status());
        if (request.hireDate() != null) employee.setHireDate(request.hireDate());
        if (request.exitDate() != null) employee.setExitDate(request.exitDate());
        if (request.skills() != null) employee.setSkills(request.skills());
        if (request.ctc() != null) employee.setCtc(request.ctc());
        if (request.address() != null) employee.setAddress(request.address());
        if (request.city() != null) employee.setCity(request.city());
        if (request.state() != null) employee.setState(request.state());
        if (request.dateOfBirth() != null) employee.setDateOfBirth(request.dateOfBirth());
        if (request.gender() != null) employee.setGender(request.gender());

        if (request.departmentId() != null) {
            Department dept = departmentRepository.findById(request.departmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.departmentId()));
            employee.setDepartment(dept);
        }
        if (request.managerId() != null) {
            Employee manager = employeeRepository.findById(request.managerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager", "id", request.managerId()));
            employee.setManager(manager);
        }

        employee = employeeRepository.save(employee);
        log.info("Updated employee: {} ({})", employee.getFullName(), employee.getEmpCode());
        return toResponse(employee);
    }

    /**
     * Gets a single employee by ID.
     */
    @Transactional(readOnly = true)
    public EmployeeResponse getEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));
        return toResponse(employee);
    }

    /**
     * Gets all employees with multi-filter support.
     */
    @Transactional(readOnly = true)
    public PagedResponse<EmployeeSummaryResponse> getAllEmployees(
            Long departmentId, EmployeeStatus status, String search, Pageable pageable) {
        Page<Employee> page = employeeRepository.findAllWithFilters(departmentId, status, search, pageable);
        return toPagedSummaryResponse(page);
    }

    /**
     * Searches employees by name or email.
     */
    @Transactional(readOnly = true)
    public PagedResponse<EmployeeSummaryResponse> searchEmployees(String query, Pageable pageable) {
        Page<Employee> page = employeeRepository.searchByNameOrEmail(query, pageable);
        return toPagedSummaryResponse(page);
    }

    /**
     * Gets direct reports for a manager.
     */
    @Transactional(readOnly = true)
    public List<EmployeeSummaryResponse> getDirectReports(Long managerId) {
        return employeeRepository.findAllByManagerId(managerId).stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    /**
     * Soft-deletes an employee (sets status to TERMINATED).
     */
    @Transactional
    public void deactivateEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));
        employee.setStatus(EmployeeStatus.TERMINATED);
        employeeRepository.save(employee);
        log.info("Deactivated employee: {} ({})", employee.getFullName(), employee.getEmpCode());
    }

    /**
     * Gets employee count stats by department.
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getDepartmentStats() {
        Map<String, Long> stats = new HashMap<>();
        departmentRepository.findAllByActiveTrue().forEach(dept ->
                stats.put(dept.getName(), employeeRepository.countByDepartmentId(dept.getId())));
        return stats;
    }

    // ─── Helpers ───

    private String generateEmpCode() {
        Integer maxNum = employeeRepository.findMaxEmpCodeNumber();
        int nextNum = (maxNum != null ? maxNum : 0) + 1;
        return String.format("NEX-%04d", nextNum);
    }

    private EmployeeResponse toResponse(Employee e) {
        return EmployeeResponse.builder()
                .id(e.getId())
                .empCode(e.getEmpCode())
                .firstName(e.getFirstName())
                .lastName(e.getLastName())
                .fullName(e.getFullName())
                .email(e.getEmail())
                .phone(e.getPhone())
                .departmentId(e.getDepartment() != null ? e.getDepartment().getId() : null)
                .departmentName(e.getDepartment() != null ? e.getDepartment().getName() : null)
                .managerId(e.getManager() != null ? e.getManager().getId() : null)
                .managerName(e.getManager() != null ? e.getManager().getFullName() : null)
                .designation(e.getDesignation())
                .status(e.getStatus())
                .hireDate(e.getHireDate())
                .exitDate(e.getExitDate())
                .skills(e.getSkills())
                .ctc(e.getCtc())
                .profilePhotoUrl(e.getProfilePhotoUrl())
                .address(e.getAddress())
                .city(e.getCity())
                .state(e.getState())
                .dateOfBirth(e.getDateOfBirth())
                .gender(e.getGender())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    private EmployeeSummaryResponse toSummaryResponse(Employee e) {
        return EmployeeSummaryResponse.builder()
                .id(e.getId())
                .empCode(e.getEmpCode())
                .fullName(e.getFullName())
                .email(e.getEmail())
                .departmentName(e.getDepartment() != null ? e.getDepartment().getName() : null)
                .designation(e.getDesignation())
                .status(e.getStatus())
                .profilePhotoUrl(e.getProfilePhotoUrl())
                .build();
    }

    private PagedResponse<EmployeeSummaryResponse> toPagedSummaryResponse(Page<Employee> page) {
        return PagedResponse.<EmployeeSummaryResponse>builder()
                .content(page.getContent().stream().map(this::toSummaryResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
