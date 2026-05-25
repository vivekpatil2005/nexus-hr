package com.nexushr.attendance.service;

import com.nexushr.attendance.dto.LeaveApprovalRequest;
import com.nexushr.attendance.dto.LeaveBalanceResponse;
import com.nexushr.attendance.dto.LeaveRequestDto;
import com.nexushr.attendance.dto.LeaveResponse;
import com.nexushr.attendance.entity.LeaveBalance;
import com.nexushr.attendance.entity.LeaveRequest;
import com.nexushr.attendance.entity.LeaveType;
import com.nexushr.attendance.repository.LeaveBalanceRepository;
import com.nexushr.attendance.repository.LeaveRequestRepository;
import com.nexushr.attendance.repository.LeaveTypeRepository;
import com.nexushr.auth.entity.User;
import com.nexushr.auth.repository.UserRepository;
import com.nexushr.common.audit.Auditable;
import com.nexushr.common.dto.PagedResponse;
import com.nexushr.common.enums.LeaveStatus;
import com.nexushr.common.exception.BusinessRuleViolationException;
import com.nexushr.common.exception.ResourceNotFoundException;
import com.nexushr.common.security.SecurityUtils;
import com.nexushr.employee.entity.Employee;
import com.nexushr.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    @Transactional
    @Auditable(action = "APPLY_LEAVE", entity = "LeaveRequest")
    public LeaveResponse applyForLeave(LeaveRequestDto requestDto) {
        Employee employee = getCurrentEmployee();

        if (requestDto.toDate().isBefore(requestDto.fromDate())) {
            throw new BusinessRuleViolationException("INVALID_DATE_RANGE", "Leave end date cannot be before start date.");
        }

        long daysCount = ChronoUnit.DAYS.between(requestDto.fromDate(), requestDto.toDate()) + 1;
        BigDecimal totalDays = BigDecimal.valueOf(daysCount);

        LeaveType leaveType = leaveTypeRepository.findById(requestDto.leaveTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("LeaveType", "id", requestDto.leaveTypeId()));

        if (!leaveType.getActive()) {
            throw new BusinessRuleViolationException("INACTIVE_LEAVE_TYPE", "The selected leave type is not active.");
        }

        int year = requestDto.fromDate().getYear();
        LeaveBalance balance = getOrCreateLeaveBalance(employee, leaveType, year);

        // Check if balance is sufficient (for paid leaves)
        if (leaveType.getIsPaid()) {
            BigDecimal available = balance.getTotalDays()
                    .subtract(balance.getUsedDays())
                    .subtract(balance.getPendingDays());

            if (available.compareTo(totalDays) < 0) {
                throw new BusinessRuleViolationException("INSUFFICIENT_BALANCE", 
                        String.format("Insufficient leave balance. Requested: %s days, Available: %s days.", totalDays, available));
            }
        }

        // Reserve days as pending
        balance.setPendingDays(balance.getPendingDays().add(totalDays));
        leaveBalanceRepository.save(balance);

        Employee approver = employee.getManager();
        if (approver == null) {
            // Default to department head or admin if no manager is assigned
            approver = (employee.getDepartment() != null && employee.getDepartment().getHeadId() != null)
                    ? employeeRepository.findById(employee.getDepartment().getHeadId()).orElse(null)
                    : null;
        }

        LeaveRequest leaveRequest = LeaveRequest.builder()
                .employee(employee)
                .leaveType(leaveType)
                .fromDate(requestDto.fromDate())
                .toDate(requestDto.toDate())
                .totalDays(totalDays)
                .status(LeaveStatus.PENDING)
                .approver(approver)
                .reason(requestDto.reason())
                .build();

        leaveRequest = leaveRequestRepository.save(leaveRequest);
        log.info("Leave request submitted by employee {} for {} days of {}", employee.getFullName(), totalDays, leaveType.getName());
        return toResponse(leaveRequest);
    }

    @Transactional
    @Auditable(action = "REVIEW_LEAVE", entity = "LeaveRequest")
    public LeaveResponse reviewLeaveRequest(Long id, LeaveApprovalRequest approvalRequest) {
        Employee currentEmployee = getCurrentEmployee();
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", "id", id));

        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new BusinessRuleViolationException("INVALID_STATUS", "Only pending leave requests can be approved or rejected.");
        }

        // Auth check: Only approver, admin, or HR can review
        boolean isAuthorized = SecurityUtils.isAdmin() || SecurityUtils.isHrManager() || 
                (leaveRequest.getApprover() != null && leaveRequest.getApprover().getId().equals(currentEmployee.getId()));

        if (!isAuthorized) {
            throw new BusinessRuleViolationException("UNAUTHORIZED_APPROVER", "You are not authorized to approve or reject this leave request.");
        }

        LeaveBalance balance = getOrCreateLeaveBalance(
                leaveRequest.getEmployee(), 
                leaveRequest.getLeaveType(), 
                leaveRequest.getFromDate().getYear()
        );

        if (approvalRequest.approved()) {
            leaveRequest.setStatus(LeaveStatus.APPROVED);
            leaveRequest.setApprovedAt(LocalDateTime.now());
            leaveRequest.setApprover(currentEmployee);

            // Update balance: deduct from pending, add to used
            balance.setPendingDays(balance.getPendingDays().subtract(leaveRequest.getTotalDays()));
            balance.setUsedDays(balance.getUsedDays().add(leaveRequest.getTotalDays()));
            leaveBalanceRepository.save(balance);

            log.info("Leave request id {} approved by {}", id, currentEmployee.getFullName());
        } else {
            leaveRequest.setStatus(LeaveStatus.REJECTED);
            leaveRequest.setRejectionReason(approvalRequest.rejectionReason());
            leaveRequest.setApprover(currentEmployee);

            // Update balance: deduct from pending (release back to available)
            balance.setPendingDays(balance.getPendingDays().subtract(leaveRequest.getTotalDays()));
            leaveBalanceRepository.save(balance);

            log.info("Leave request id {} rejected by {}", id, currentEmployee.getFullName());
        }

        leaveRequest = leaveRequestRepository.save(leaveRequest);
        return toResponse(leaveRequest);
    }

    @Transactional
    @Auditable(action = "CANCEL_LEAVE", entity = "LeaveRequest")
    public LeaveResponse cancelLeaveRequest(Long id) {
        Employee currentEmployee = getCurrentEmployee();
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", "id", id));

        // Auth check: only the employee who applied can cancel
        if (!leaveRequest.getEmployee().getId().equals(currentEmployee.getId())) {
            throw new BusinessRuleViolationException("UNAUTHORIZED_CANCELLATION", "You can only cancel your own leave requests.");
        }

        if (leaveRequest.getStatus() == LeaveStatus.CANCELLED || leaveRequest.getStatus() == LeaveStatus.REJECTED) {
            throw new BusinessRuleViolationException("INVALID_STATUS", "Leave request is already in a final state: " + leaveRequest.getStatus());
        }

        LeaveBalance balance = getOrCreateLeaveBalance(
                leaveRequest.getEmployee(), 
                leaveRequest.getLeaveType(), 
                leaveRequest.getFromDate().getYear()
        );

        if (leaveRequest.getStatus() == LeaveStatus.PENDING) {
            // Deduct from pending (release back)
            balance.setPendingDays(balance.getPendingDays().subtract(leaveRequest.getTotalDays()));
        } else if (leaveRequest.getStatus() == LeaveStatus.APPROVED) {
            // Check if leave has already started/passed
            if (leaveRequest.getFromDate().isBefore(LocalDate.now())) {
                throw new BusinessRuleViolationException("PAST_LEAVE_CANCELLATION", "Cannot cancel an approved leave request that has already started or passed.");
            }
            // Deduct from used (release back)
            balance.setUsedDays(balance.getUsedDays().subtract(leaveRequest.getTotalDays()));
        }

        leaveRequest.setStatus(LeaveStatus.CANCELLED);
        leaveBalanceRepository.save(balance);
        leaveRequest = leaveRequestRepository.save(leaveRequest);

        log.info("Leave request id {} cancelled by employee", id);
        return toResponse(leaveRequest);
    }

    @Transactional
    public List<LeaveBalanceResponse> getMyLeaveBalances() {
        Employee employee = getCurrentEmployee();
        int year = LocalDate.now().getYear();
        return getLeaveBalancesForEmployee(employee.getId(), year);
    }

    @Transactional
    public List<LeaveBalanceResponse> getLeaveBalancesForEmployee(Long employeeId, Integer year) {
        int targetYear = year != null ? year : LocalDate.now().getYear();
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));

        // Pre-create balances for all active leave types if not exists
        List<LeaveType> activeTypes = leaveTypeRepository.findAllByActiveTrue();
        for (LeaveType type : activeTypes) {
            getOrCreateLeaveBalance(employee, type, targetYear);
        }

        return leaveBalanceRepository.findAllByEmployeeIdAndYear(employeeId, targetYear).stream()
                .map(this::toBalanceResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PagedResponse<LeaveResponse> filterLeaveRequests(
            Long employeeId, LeaveStatus status, Long approverId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        
        // Security check: If the user is not Admin, HR, or Manager, force employeeId to be current employee
        if (!SecurityUtils.isAdmin() && !SecurityUtils.isHrManager() && !SecurityUtils.isManager()) {
            Employee currentEmployee = getCurrentEmployee();
            employeeId = currentEmployee.getId();
        }

        Page<LeaveRequest> page = leaveRequestRepository.filterLeaveRequests(employeeId, status, approverId, startDate, endDate, pageable);
        return PagedResponse.<LeaveResponse>builder()
                .content(page.getContent().stream().map(this::toResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public List<LeaveResponse> getPendingApprovals() {
        Employee currentEmployee = getCurrentEmployee();
        return leaveRequestRepository.findAllByApproverIdAndStatus(currentEmployee.getId(), LeaveStatus.PENDING).stream()
                .map(this::toResponse)
                .toList();
    }

    // ─── Helper Methods ───

    private LeaveBalance getOrCreateLeaveBalance(Employee employee, LeaveType leaveType, int year) {
        return leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndYear(employee.getId(), leaveType.getId(), year)
                .orElseGet(() -> {
                    LeaveBalance newBalance = LeaveBalance.builder()
                            .employee(employee)
                            .leaveType(leaveType)
                            .year(year)
                            .totalDays(BigDecimal.valueOf(leaveType.getDefaultDays()))
                            .usedDays(BigDecimal.ZERO)
                            .pendingDays(BigDecimal.ZERO)
                            .build();
                    return leaveBalanceRepository.save(newBalance);
                });
    }

    private Employee getCurrentEmployee() {
        String username = SecurityUtils.getCurrentUsername()
                .orElseThrow(() -> new BusinessRuleViolationException("UNAUTHENTICATED", "No authenticated user session found."));

        User user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        if (user.getEmployeeId() == null) {
            throw new BusinessRuleViolationException("NO_ASSOCIATED_EMPLOYEE", "Logged in user does not have an associated employee record.");
        }

        return employeeRepository.findById(user.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", user.getEmployeeId()));
    }

    private LeaveResponse toResponse(LeaveRequest request) {
        return LeaveResponse.builder()
                .id(request.getId())
                .employeeId(request.getEmployee().getId())
                .employeeName(request.getEmployee().getFullName())
                .leaveTypeId(request.getLeaveType().getId())
                .leaveTypeName(request.getLeaveType().getName())
                .fromDate(request.getFromDate())
                .toDate(request.getToDate())
                .totalDays(request.getTotalDays())
                .status(request.getStatus())
                .approverId(request.getApprover() != null ? request.getApprover().getId() : null)
                .approverName(request.getApprover() != null ? request.getApprover().getFullName() : null)
                .reason(request.getReason())
                .rejectionReason(request.getRejectionReason())
                .approvedAt(request.getApprovedAt())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }

    private LeaveBalanceResponse toBalanceResponse(LeaveBalance balance) {
        return LeaveBalanceResponse.builder()
                .id(balance.getId())
                .leaveTypeId(balance.getLeaveType().getId())
                .leaveTypeName(balance.getLeaveType().getName())
                .year(balance.getYear())
                .totalDays(balance.getTotalDays())
                .usedDays(balance.getUsedDays())
                .pendingDays(balance.getPendingDays())
                .build();
    }
}
