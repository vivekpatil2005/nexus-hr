package com.nexushr.attendance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexushr.attendance.dto.AttendanceResponse;
import com.nexushr.attendance.dto.CheckInRequest;
import com.nexushr.attendance.dto.CheckOutRequest;
import com.nexushr.attendance.entity.AttendanceRecord;
import com.nexushr.attendance.repository.AttendanceRecordRepository;
import com.nexushr.auth.entity.User;
import com.nexushr.auth.repository.UserRepository;
import com.nexushr.common.dto.PagedResponse;
import com.nexushr.common.enums.AttendanceStatus;
import com.nexushr.common.exception.BusinessRuleViolationException;
import com.nexushr.common.exception.ResourceNotFoundException;
import com.nexushr.common.security.SecurityUtils;
import com.nexushr.employee.entity.Employee;
import com.nexushr.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRecordRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public static final String ATTENDANCE_CHANNEL = "attendance-live-stream";

    @Transactional
    public AttendanceResponse checkIn(CheckInRequest request) {
        Employee employee = getCurrentEmployee();
        LocalDate today = LocalDate.now();

        if (attendanceRepository.findByEmployeeIdAndDate(employee.getId(), today).isPresent()) {
            throw new BusinessRuleViolationException("ALREADY_CHECKED_IN", "Employee is already checked in for today.");
        }

        AttendanceRecord record = AttendanceRecord.builder()
                .employee(employee)
                .date(today)
                .checkIn(LocalDateTime.now())
                .status(AttendanceStatus.PRESENT)
                .overtimeHours(BigDecimal.ZERO)
                .notes(request != null ? request.notes() : null)
                .build();

        record = attendanceRepository.save(record);
        AttendanceResponse response = toResponse(record);
        publishEvent(response);
        log.info("Employee {} checked in at {}", employee.getFullName(), record.getCheckIn());
        return response;
    }

    @Transactional
    public AttendanceResponse checkOut(CheckOutRequest request) {
        Employee employee = getCurrentEmployee();
        LocalDate today = LocalDate.now();

        AttendanceRecord record = attendanceRepository.findByEmployeeIdAndDate(employee.getId(), today)
                .orElseThrow(() -> new BusinessRuleViolationException("NOT_CHECKED_IN", "No check-in record found for today. Please check-in first."));

        if (record.getCheckOut() != null) {
            throw new BusinessRuleViolationException("ALREADY_CHECKED_OUT", "Employee has already checked out for today.");
        }

        record.setCheckOut(LocalDateTime.now());
        if (request != null && request.notes() != null) {
            String combinedNotes = record.getNotes() == null ? request.notes() : record.getNotes() + " | Check-out: " + request.notes();
            record.setNotes(combinedNotes);
        }

        // Calculate work hours and overtime (standard day is 8 hours)
        Duration duration = Duration.between(record.getCheckIn(), record.getCheckOut());
        double hours = duration.toMinutes() / 60.0;
        if (hours > 8.0) {
            BigDecimal ot = BigDecimal.valueOf(hours - 8.0).setScale(2, RoundingMode.HALF_UP);
            record.setOvertimeHours(ot);
        } else {
            record.setOvertimeHours(BigDecimal.ZERO);
        }

        // If employee worked less than 4 hours, status is HALF_DAY
        if (hours < 4.0) {
            record.setStatus(AttendanceStatus.HALF_DAY);
        }

        record = attendanceRepository.save(record);
        AttendanceResponse response = toResponse(record);
        publishEvent(response);
        log.info("Employee {} checked out at {} (Overtime: {} hours)", employee.getFullName(), record.getCheckOut(), record.getOvertimeHours());
        return response;
    }

    @Transactional(readOnly = true)
    public AttendanceResponse getTodayAttendance() {
        Employee employee = getCurrentEmployee();
        AttendanceRecord record = attendanceRepository.findByEmployeeIdAndDate(employee.getId(), LocalDate.now())
                .orElseThrow(() -> new ResourceNotFoundException("AttendanceRecord", "date", LocalDate.now()));
        return toResponse(record);
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getMyAttendanceHistory(LocalDate startDate, LocalDate endDate) {
        Employee employee = getCurrentEmployee();
        LocalDate start = startDate != null ? startDate : LocalDate.now().minusDays(30);
        LocalDate end = endDate != null ? endDate : LocalDate.now();

        return attendanceRepository.findAllByEmployeeIdAndDateBetween(employee.getId(), start, end).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PagedResponse<AttendanceResponse> getAttendanceReport(
            Long employeeId, LocalDate startDate, LocalDate endDate, AttendanceStatus status, Pageable pageable) {
        Page<AttendanceRecord> page = attendanceRepository.filterAttendance(employeeId, startDate, endDate, status, pageable);
        return PagedResponse.<AttendanceResponse>builder()
                .content(page.getContent().stream().map(this::toResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    // ─── Helper Methods ───

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

    private void publishEvent(AttendanceResponse response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.convertAndSend(ATTENDANCE_CHANNEL, json);
        } catch (Exception e) {
            log.error("Failed to publish attendance event to Redis channel", e);
        }
    }

    private AttendanceResponse toResponse(AttendanceRecord record) {
        return AttendanceResponse.builder()
                .id(record.getId())
                .employeeId(record.getEmployee().getId())
                .employeeName(record.getEmployee().getFullName())
                .empCode(record.getEmployee().getEmpCode())
                .date(record.getDate())
                .checkIn(record.getCheckIn())
                .checkOut(record.getCheckOut())
                .status(record.getStatus())
                .overtimeHours(record.getOvertimeHours())
                .notes(record.getNotes())
                .build();
    }
}
