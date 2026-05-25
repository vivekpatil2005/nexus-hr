package com.nexushr.attendance.repository;

import com.nexushr.attendance.entity.LeaveRequest;
import com.nexushr.common.enums.LeaveStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest> findAllByEmployeeId(Long employeeId);

    List<LeaveRequest> findAllByApproverIdAndStatus(Long approverId, LeaveStatus status);

    @Query("SELECT lr FROM LeaveRequest lr WHERE " +
           "(:employeeId IS NULL OR lr.employee.id = :employeeId) AND " +
           "(:status IS NULL OR lr.status = :status) AND " +
           "(:approverId IS NULL OR lr.approver.id = :approverId) AND " +
           "(:startDate IS NULL OR lr.fromDate >= :startDate) AND " +
           "(:endDate IS NULL OR lr.toDate <= :endDate)")
    Page<LeaveRequest> filterLeaveRequests(
            @Param("employeeId") Long employeeId,
            @Param("status") LeaveStatus status,
            @Param("approverId") Long approverId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);
}
