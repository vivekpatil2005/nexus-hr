package com.nexushr.attendance.repository;

import com.nexushr.attendance.entity.AttendanceRecord;
import com.nexushr.common.enums.AttendanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    Optional<AttendanceRecord> findByEmployeeIdAndDate(Long employeeId, LocalDate date);

    List<AttendanceRecord> findAllByEmployeeIdAndDateBetween(Long employeeId, LocalDate startDate, LocalDate endDate);

    List<AttendanceRecord> findAllByDate(LocalDate date);

    @Query("SELECT ar FROM AttendanceRecord ar WHERE " +
           "(:employeeId IS NULL OR ar.employee.id = :employeeId) AND " +
           "(:startDate IS NULL OR ar.date >= :startDate) AND " +
           "(:endDate IS NULL OR ar.date <= :endDate) AND " +
           "(:status IS NULL OR ar.status = :status)")
    Page<AttendanceRecord> filterAttendance(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") AttendanceStatus status,
            Pageable pageable);
}
