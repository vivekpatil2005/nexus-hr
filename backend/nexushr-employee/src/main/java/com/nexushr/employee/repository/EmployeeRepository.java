package com.nexushr.employee.repository;

import com.nexushr.common.enums.EmployeeStatus;
import com.nexushr.employee.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Employee} entity with pagination, search, and stats queries.
 */
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmpCode(String empCode);

    Optional<Employee> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmpCode(String empCode);

    Page<Employee> findAllByDepartmentId(Long departmentId, Pageable pageable);

    Page<Employee> findAllByStatus(EmployeeStatus status, Pageable pageable);

    List<Employee> findAllByManagerId(Long managerId);

    /**
     * Search employees by first name, last name, or email (case-insensitive).
     */
    @Query("SELECT e FROM Employee e WHERE " +
           "LOWER(e.firstName) LIKE LOWER(CONCAT('%', CAST(:query AS string), '%')) OR " +
           "LOWER(e.lastName) LIKE LOWER(CONCAT('%', CAST(:query AS string), '%')) OR " +
           "LOWER(e.email) LIKE LOWER(CONCAT('%', CAST(:query AS string), '%')) OR " +
           "LOWER(e.empCode) LIKE LOWER(CONCAT('%', CAST(:query AS string), '%'))")
    Page<Employee> searchByNameOrEmail(@Param("query") String query, Pageable pageable);

    /**
     * Multi-filter search for employees.
     */
    @Query("SELECT e FROM Employee e WHERE " +
           "(:departmentId IS NULL OR e.department.id = :departmentId) AND " +
           "(:status IS NULL OR e.status = :status) AND " +
           "(:search IS NULL OR LOWER(e.firstName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
           "OR LOWER(e.lastName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
           "OR LOWER(e.email) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))")
    Page<Employee> findAllWithFilters(
            @Param("departmentId") Long departmentId,
            @Param("status") EmployeeStatus status,
            @Param("search") String search,
            Pageable pageable);

    long countByDepartmentId(Long departmentId);

    long countByStatus(EmployeeStatus status);

    @Query("SELECT MAX(CAST(SUBSTRING(e.empCode, 5, LENGTH(e.empCode)) AS int)) FROM Employee e")
    Integer findMaxEmpCodeNumber();
}
