package com.nexushr.payroll.controller;

import com.nexushr.auth.entity.User;
import com.nexushr.auth.repository.UserRepository;
import com.nexushr.common.dto.ApiResponse;
import com.nexushr.common.exception.BusinessRuleViolationException;
import com.nexushr.common.exception.ResourceNotFoundException;
import com.nexushr.payroll.dto.*;
import com.nexushr.payroll.service.PayrollService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payroll")
@Tag(name = "Payroll", description = "Endpoints for salary structures, payroll runs, and employee payslips")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;
    private final UserRepository userRepository;

    // ─── Salary Structures ───

    @PostMapping("/salary-structures")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @Operation(summary = "Create or update an employee's salary structure")
    public ResponseEntity<ApiResponse<SalaryStructureResponse>> saveSalaryStructure(
            @Valid @RequestBody SalaryStructureRequest request) {
        SalaryStructureResponse response = payrollService.saveSalaryStructure(request);
        return ResponseEntity.ok(ApiResponse.success("Salary structure saved successfully", response));
    }

    @GetMapping("/salary-structures/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'MANAGER')")
    @Operation(summary = "Get active salary structure for a specific employee")
    public ResponseEntity<ApiResponse<SalaryStructureResponse>> getSalaryStructure(
            @PathVariable Long employeeId) {
        SalaryStructureResponse response = payrollService.getSalaryStructureByEmployeeId(employeeId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ─── Payroll Runs ───

    @PostMapping("/runs")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @Operation(summary = "Initiate and run payroll processing for a month/year")
    public ResponseEntity<ApiResponse<PayrollRunResponse>> initiatePayrollRun(
            @Valid @RequestBody PayrollRunRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        PayrollRunResponse response = payrollService.initiatePayrollRun(request, username);
        return ResponseEntity.ok(ApiResponse.success("Payroll run initiated successfully", response));
    }

    @GetMapping("/runs")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @Operation(summary = "Get all payroll runs")
    public ResponseEntity<ApiResponse<List<PayrollRunResponse>>> getPayrollRuns() {
        List<PayrollRunResponse> runs = payrollService.getPayrollRuns();
        return ResponseEntity.ok(ApiResponse.success(runs));
    }

    @GetMapping("/runs/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @Operation(summary = "Get details of a specific payroll run")
    public ResponseEntity<ApiResponse<PayrollRunResponse>> getPayrollRunById(@PathVariable Long id) {
        PayrollRunResponse run = payrollService.getPayrollRunById(id);
        return ResponseEntity.ok(ApiResponse.success(run));
    }

    @PostMapping("/runs/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @Operation(summary = "Approve a completed payroll run")
    public ResponseEntity<ApiResponse<PayrollRunResponse>> approvePayrollRun(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        PayrollRunResponse run = payrollService.approvePayrollRun(id, username);
        return ResponseEntity.ok(ApiResponse.success("Payroll run approved successfully", run));
    }

    @PostMapping("/runs/{id}/lock")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @Operation(summary = "Lock an approved payroll run")
    public ResponseEntity<ApiResponse<PayrollRunResponse>> lockPayrollRun(@PathVariable Long id) {
        PayrollRunResponse run = payrollService.lockPayrollRun(id);
        return ResponseEntity.ok(ApiResponse.success("Payroll run locked successfully", run));
    }

    // ─── Payslips ───

    @GetMapping("/runs/{id}/payslips")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @Operation(summary = "Get all payslips for a specific payroll run")
    public ResponseEntity<ApiResponse<List<PayslipResponse>>> getPayslipsByRun(@PathVariable Long id) {
        List<PayslipResponse> payslips = payrollService.getPayslipsByRunId(id);
        return ResponseEntity.ok(ApiResponse.success(payslips));
    }

    @GetMapping("/payslips/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'MANAGER')")
    @Operation(summary = "Get all payslips for a specific employee")
    public ResponseEntity<ApiResponse<List<PayslipResponse>>> getPayslipsByEmployee(@PathVariable Long employeeId) {
        List<PayslipResponse> payslips = payrollService.getPayslipsByEmployeeId(employeeId);
        return ResponseEntity.ok(ApiResponse.success(payslips));
    }

    @GetMapping("/payslips/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get payslips for the currently logged-in employee")
    public ResponseEntity<ApiResponse<List<PayslipResponse>>> getMyPayslips() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        if (user.getEmployeeId() == null) {
            throw new BusinessRuleViolationException("Current user is not associated with an employee profile.");
        }

        List<PayslipResponse> payslips = payrollService.getPayslipsByEmployeeId(user.getEmployeeId());
        return ResponseEntity.ok(ApiResponse.success(payslips));
    }

    @GetMapping("/payslips/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get specific payslip details")
    public ResponseEntity<ApiResponse<PayslipResponse>> getPayslipById(@PathVariable Long id) {
        PayslipResponse payslip = payrollService.getPayslipById(id);
        
        // Security check: Employees can only view their own payslips
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        
        boolean isHrOrAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_HR_MANAGER"));

        if (!isHrOrAdmin && !payslip.employeeId().equals(user.getEmployeeId())) {
            throw new BusinessRuleViolationException("You are not authorized to view this payslip.");
        }

        return ResponseEntity.ok(ApiResponse.success(payslip));
    }

    @GetMapping("/payslips/{id}/download")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Download payslip PDF")
    public ResponseEntity<byte[]> downloadPayslip(@PathVariable Long id) {
        // Security check: Employees can only view/download their own payslips
        PayslipResponse payslip = payrollService.getPayslipById(id);
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        
        boolean isHrOrAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_HR_MANAGER"));

        if (!isHrOrAdmin && !payslip.employeeId().equals(user.getEmployeeId())) {
            throw new BusinessRuleViolationException("You are not authorized to download this payslip.");
        }

        byte[] pdfBytes = payrollService.getPayslipPdf(id);
        String fileName = "payslip_" + payslip.periodMonth() + "_" + payslip.periodYear() + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/payslips/download-by-key")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Download payslip PDF by storage key")
    public ResponseEntity<byte[]> downloadPayslipByKey(@RequestParam String key) {
        // Typically, this key contains payrollRunId and employeeId, e.g. "payslip_1_2.pdf"
        // Let's secure it.
        byte[] pdfBytes = payrollService.getPayslipPdfByKey(key);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"payslip.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
