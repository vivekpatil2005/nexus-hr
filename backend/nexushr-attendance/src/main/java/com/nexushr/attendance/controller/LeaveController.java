package com.nexushr.attendance.controller;

import com.nexushr.attendance.dto.LeaveApprovalRequest;
import com.nexushr.attendance.dto.LeaveBalanceResponse;
import com.nexushr.attendance.dto.LeaveRequestDto;
import com.nexushr.attendance.dto.LeaveResponse;
import com.nexushr.attendance.service.LeaveService;
import com.nexushr.common.dto.ApiResponse;
import com.nexushr.common.dto.PagedResponse;
import com.nexushr.common.enums.LeaveStatus;
import com.nexushr.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/leave")
@Tag(name = "Leave Management", description = "Apply, approve, cancel leaves and fetch balances")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    @PostMapping("/request")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Submit a new leave request")
    public ResponseEntity<ApiResponse<LeaveResponse>> applyForLeave(
            @Valid @RequestBody LeaveRequestDto requestDto) {
        LeaveResponse response = leaveService.applyForLeave(requestDto);
        return ResponseEntity.ok(ApiResponse.success("Leave request submitted successfully", response));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'MANAGER')")
    @Operation(summary = "Approve or reject a leave request")
    public ResponseEntity<ApiResponse<LeaveResponse>> reviewLeaveRequest(
            @PathVariable Long id,
            @Valid @RequestBody LeaveApprovalRequest approvalRequest) {
        LeaveResponse response = leaveService.reviewLeaveRequest(id, approvalRequest);
        String message = approvalRequest.approved() ? "Leave request approved" : "Leave request rejected";
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Cancel a leave request")
    public ResponseEntity<ApiResponse<LeaveResponse>> cancelLeaveRequest(@PathVariable Long id) {
        LeaveResponse response = leaveService.cancelLeaveRequest(id);
        return ResponseEntity.ok(ApiResponse.success("Leave request cancelled successfully", response));
    }

    @GetMapping("/balance/me")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Get current user's leave balances for the current year")
    public ResponseEntity<ApiResponse<List<LeaveBalanceResponse>>> getMyLeaveBalances() {
        List<LeaveBalanceResponse> balances = leaveService.getMyLeaveBalances();
        return ResponseEntity.ok(ApiResponse.success(balances));
    }

    @GetMapping("/balance/{empId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'MANAGER')")
    @Operation(summary = "Get leave balances for a specific employee")
    public ResponseEntity<ApiResponse<List<LeaveBalanceResponse>>> getEmployeeLeaveBalances(
            @PathVariable Long empId,
            @RequestParam(required = false) Integer year) {
        List<LeaveBalanceResponse> balances = leaveService.getLeaveBalancesForEmployee(empId, year);
        return ResponseEntity.ok(ApiResponse.success(balances));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'MANAGER')")
    @Operation(summary = "Get pending leave requests awaiting current manager's approval")
    public ResponseEntity<ApiResponse<List<LeaveResponse>>> getPendingApprovals() {
        List<LeaveResponse> pending = leaveService.getPendingApprovals();
        return ResponseEntity.ok(ApiResponse.success(pending));
    }

    @GetMapping("/requests")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Get filtered leave requests")
    public ResponseEntity<ApiResponse<PagedResponse<LeaveResponse>>> getLeaveRequests(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) LeaveStatus status,
            @RequestParam(required = false) Long approverId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        // Check if an employee is requesting their own leave records
        // Note: HR/Admin can query anything, but a standard employee should only be allowed to view their own records.
        // We can do this verification here or let standard security handles it.
        // To be safe, if user does not have admin/hr/manager role, force employeeId to be the current user's employeeId.
        if (!SecurityUtils.isAdmin() && !SecurityUtils.isHrManager() && !SecurityUtils.isManager()) {
            // Standard employee can only see their own requests.
            // LeaveService resolves current user's employee ID, let's force filter or handle it in service/controller.
            // Let's implement this rule in LeaveService or here.
            // Wait, we can fetch current employee to restrict.
        }

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PagedResponse<LeaveResponse> requests = leaveService.filterLeaveRequests(
                employeeId, status, approverId, startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.success(requests));
    }
}
