package com.nexushr.attendance.repository;

import com.nexushr.attendance.entity.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveTypeRepository extends JpaRepository<LeaveType, Long> {

    List<LeaveType> findAllByActiveTrue();

    Optional<LeaveType> findByName(String name);
}
