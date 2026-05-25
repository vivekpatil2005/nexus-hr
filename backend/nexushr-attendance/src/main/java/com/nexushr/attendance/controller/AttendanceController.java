package com.nexushr.attendance.controller;

import com.nexushr.attendance.dto.AttendanceResponse;
import com.nexushr.attendance.dto.CheckInRequest;
import com.nexushr.attendance.dto.CheckOutRequest;
import com.nexushr.attendance.service.AttendanceService;
import com.nexushr.common.dto.ApiResponse;
import com.nexushr.common.dto.PagedResponse;
import com.nexushr.common.enums.AttendanceStatus;
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
@RequestMapping("/api/attendance")
@Tag(name = "Attendance", description = "Employee check-in/out and attendance reporting")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/check-in")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Check in for the current day")
    public ResponseEntity<ApiResponse<AttendanceResponse>> checkIn(
            @Valid @RequestBody(required = false) CheckInRequest request) {
        AttendanceResponse response = attendanceService.checkIn(request);
        return ResponseEntity.ok(ApiResponse.success("Checked in successfully", response));
    }

    @PostMapping("/check-out")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Check out for the current day")
    public ResponseEntity<ApiResponse<AttendanceResponse>> checkOut(
            @Valid @RequestBody(required = false) CheckOutRequest request) {
        AttendanceResponse response = attendanceService.checkOut(request);
        return ResponseEntity.ok(ApiResponse.success("Checked out successfully", response));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Get current employee's attendance history")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getMyAttendance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<AttendanceResponse> history = attendanceService.getMyAttendanceHistory(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @GetMapping("/today")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Get current employee's attendance status for today")
    public ResponseEntity<ApiResponse<AttendanceResponse>> getTodayAttendance() {
        try {
            AttendanceResponse response = attendanceService.getTodayAttendance();
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound | com.nexushr.common.exception.ResourceNotFoundException e) {
            return ResponseEntity.ok(ApiResponse.success("No attendance record found for today.", null));
        }
    }

    @GetMapping("/report")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'MANAGER')")
    @Operation(summary = "Get filtered attendance report for managers/HR")
    public ResponseEntity<ApiResponse<PagedResponse<AttendanceResponse>>> getAttendanceReport(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) AttendanceStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PagedResponse<AttendanceResponse> report = attendanceService.getAttendanceReport(
                employeeId, startDate, endDate, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(report));
    }
}
