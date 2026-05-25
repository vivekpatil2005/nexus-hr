package com.nexushr.employee.repository;

import com.nexushr.employee.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Department} entity operations.
 */
@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    Optional<Department> findByName(String name);

    List<Department> findByParentId(Long parentId);

    List<Department> findByParentIsNull();

    List<Department> findAllByActiveTrue();

    boolean existsByName(String name);
}
