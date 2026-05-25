package com.nexushr.payroll.service;

import com.nexushr.auth.entity.User;
import com.nexushr.auth.repository.UserRepository;
import com.nexushr.common.enums.PayrollRunStatus;
import com.nexushr.common.exception.BusinessRuleViolationException;
import com.nexushr.common.exception.ResourceNotFoundException;
import com.nexushr.employee.entity.Employee;
import com.nexushr.employee.repository.EmployeeRepository;
import com.nexushr.payroll.dto.*;
import com.nexushr.payroll.entity.PayrollRun;
import com.nexushr.payroll.entity.Payslip;
import com.nexushr.payroll.entity.SalaryStructure;
import com.nexushr.payroll.repository.PayrollRunRepository;
import com.nexushr.payroll.repository.PayslipRepository;
import com.nexushr.payroll.repository.SalaryStructureRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class PayrollService {

    private static final Logger log = LoggerFactory.getLogger(PayrollService.class);

    @Autowired
    private SalaryStructureRepository salaryStructureRepository;

    @Autowired
    private PayrollRunRepository payrollRunRepository;

    @Autowired
    private PayslipRepository payslipRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StorageService storageService;

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job payrollJob;

    // ─── Salary Structure Operations ───

    @Transactional
    public SalaryStructureResponse saveSalaryStructure(SalaryStructureRequest request) {
        Employee employee = employeeRepository.findById(request.employeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", request.employeeId()));

        // Deactivate existing active structures
        salaryStructureRepository.findByEmployeeIdAndActiveTrue(request.employeeId())
                .ifPresent(existing -> {
                    existing.setActive(false);
                    existing.setEffectiveTo(request.effectiveFrom().minusDays(1));
                    salaryStructureRepository.save(existing);
                });

        SalaryStructure structure = SalaryStructure.builder()
                .employee(employee)
                .effectiveFrom(request.effectiveFrom())
                .basic(request.basic())
                .hra(request.hra() != null ? request.hra() : BigDecimal.ZERO)
                .da(request.da() != null ? request.da() : BigDecimal.ZERO)
                .specialAllowance(request.specialAllowance() != null ? request.specialAllowance() : BigDecimal.ZERO)
                .conveyance(request.conveyance() != null ? request.conveyance() : BigDecimal.ZERO)
                .medical(request.medical() != null ? request.medical() : BigDecimal.ZERO)
                .lta(request.lta() != null ? request.lta() : BigDecimal.ZERO)
                .otherAllowances(request.otherAllowances() != null ? request.otherAllowances() : BigDecimal.ZERO)
                .gross(request.gross())
                .ctc(request.ctc())
                .active(true)
                .build();

        SalaryStructure saved = salaryStructureRepository.save(structure);
        return mapToSalaryStructureResponse(saved);
    }

    public SalaryStructureResponse getSalaryStructureByEmployeeId(Long employeeId) {
        SalaryStructure structure = salaryStructureRepository.findByEmployeeIdAndActiveTrue(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("SalaryStructure", "employeeId", employeeId));
        return mapToSalaryStructureResponse(structure);
    }

    // ─── Payroll Run Operations ───

    @Transactional
    public PayrollRunResponse initiatePayrollRun(PayrollRunRequest request, String username) {
        // Check if run already exists for this period
        if (payrollRunRepository.existsByPeriodMonthAndPeriodYear(request.periodMonth(), request.periodYear())) {
            throw new BusinessRuleViolationException("Payroll has already been initiated/processed for this period: " 
                    + request.periodMonth() + "/" + request.periodYear());
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        // Create PayrollRun
        PayrollRun run = PayrollRun.builder()
                .periodMonth(request.periodMonth())
                .periodYear(request.periodYear())
                .status(PayrollRunStatus.PROCESSING)
                .notes(request.notes())
                .runBy(user)
                .startedAt(LocalDateTime.now())
                .totalGross(BigDecimal.ZERO)
                .totalDeductions(BigDecimal.ZERO)
                .totalNet(BigDecimal.ZERO)
                .employeeCount(0)
                .build();

        PayrollRun savedRun = payrollRunRepository.save(run);

        // Async Batch Job execution
        CompletableFuture.runAsync(() -> {
            try {
                JobParameters params = new JobParametersBuilder()
                        .addLong("payrollRunId", savedRun.getId())
                        .addLong("month", (long) request.periodMonth())
                        .addLong("year", (long) request.periodYear())
                        .addLong("time", System.currentTimeMillis())
                        .toJobParameters();

                jobLauncher.run(payrollJob, params);
            } catch (Exception e) {
                log.error("Failed to run payroll batch job for run ID: {}", savedRun.getId(), e);
                savedRun.setStatus(PayrollRunStatus.FAILED);
                savedRun.setCompletedAt(LocalDateTime.now());
                payrollRunRepository.save(savedRun);
            }
        });

        return mapToPayrollRunResponse(savedRun);
    }

    @Transactional(readOnly = true)
    public List<PayrollRunResponse> getPayrollRuns() {
        return payrollRunRepository.findAll().stream()
                .map(this::mapToPayrollRunResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PayrollRunResponse getPayrollRunById(Long id) {
        PayrollRun run = payrollRunRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PayrollRun", "id", id));
        return mapToPayrollRunResponse(run);
    }

    @Transactional
    public PayrollRunResponse approvePayrollRun(Long id, String approverUsername) {
        PayrollRun run = payrollRunRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PayrollRun", "id", id));

        if (run.getStatus() != PayrollRunStatus.DRAFT) {
            throw new BusinessRuleViolationException("Only DRAFT payroll runs can be approved.");
        }

        User approver = userRepository.findByUsername(approverUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", approverUsername));

        run.setStatus(PayrollRunStatus.APPROVED);
        run.setApprovedBy(approver);
        return mapToPayrollRunResponse(payrollRunRepository.save(run));
    }

    @Transactional
    public PayrollRunResponse lockPayrollRun(Long id) {
        PayrollRun run = payrollRunRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PayrollRun", "id", id));

        if (run.getStatus() != PayrollRunStatus.APPROVED) {
            throw new BusinessRuleViolationException("Only APPROVED payroll runs can be locked.");
        }

        run.setStatus(PayrollRunStatus.LOCKED);
        return mapToPayrollRunResponse(payrollRunRepository.save(run));
    }

    // ─── Payslip Operations ───

    @Transactional(readOnly = true)
    public List<PayslipResponse> getPayslipsByEmployeeId(Long employeeId) {
        return payslipRepository.findByEmployeeIdOrderByPayrollRunPeriodYearDescPayrollRunPeriodMonthDesc(employeeId)
                .stream()
                .map(this::mapToPayslipResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PayslipResponse> getPayslipsByRunId(Long runId) {
        return payslipRepository.findByPayrollRunId(runId)
                .stream()
                .map(this::mapToPayslipResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PayslipResponse getPayslipById(Long id) {
        Payslip payslip = payslipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payslip", "id", id));
        return mapToPayslipResponse(payslip);
    }

    @Transactional(readOnly = true)
    public byte[] getPayslipPdf(Long id) {
        Payslip payslip = payslipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payslip", "id", id));
        
        if (payslip.getPdfS3Key() == null) {
            throw new BusinessRuleViolationException("Payslip PDF is not generated yet.");
        }

        return storageService.downloadFile(payslip.getPdfS3Key());
    }

    @Transactional(readOnly = true)
    public byte[] getPayslipPdfByKey(String key) {
        return storageService.downloadFile(key);
    }

    // ─── Mappings ───

    private SalaryStructureResponse mapToSalaryStructureResponse(SalaryStructure structure) {
        return new SalaryStructureResponse(
                structure.getId(),
                structure.getEmployee().getId(),
                structure.getEmployee().getFullName(),
                structure.getEffectiveFrom(),
                structure.getEffectiveTo(),
                structure.getBasic(),
                structure.getHra(),
                structure.getDa(),
                structure.getSpecialAllowance(),
                structure.getConveyance(),
                structure.getMedical(),
                structure.getLta(),
                structure.getOtherAllowances(),
                structure.getGross(),
                structure.getCtc(),
                structure.isActive()
        );
    }

    private PayrollRunResponse mapToPayrollRunResponse(PayrollRun run) {
        return new PayrollRunResponse(
                run.getId(),
                run.getPeriodMonth(),
                run.getPeriodYear(),
                run.getStatus(),
                run.getTotalGross(),
                run.getTotalDeductions(),
                run.getTotalNet(),
                run.getEmployeeCount(),
                run.getRunBy() != null ? run.getRunBy().getUsername() : null,
                run.getApprovedBy() != null ? run.getApprovedBy().getUsername() : null,
                run.getNotes(),
                run.getStartedAt(),
                run.getCompletedAt(),
                run.getCreatedAt()
        );
    }

    private PayslipResponse mapToPayslipResponse(Payslip payslip) {
        return new PayslipResponse(
                payslip.getId(),
                payslip.getPayrollRun().getId(),
                payslip.getPayrollRun().getPeriodMonth(),
                payslip.getPayrollRun().getPeriodYear(),
                payslip.getEmployee().getId(),
                payslip.getEmpCode(),
                payslip.getEmployeeName(),
                payslip.getDepartment(),
                payslip.getDesignation(),
                payslip.getBasic(),
                payslip.getHra(),
                payslip.getDa(),
                payslip.getSpecialAllowance(),
                payslip.getOtherEarnings(),
                payslip.getGross(),
                payslip.getPfEmployee(),
                payslip.getPfEmployer(),
                payslip.getEsiEmployee(),
                payslip.getEsiEmployer(),
                payslip.getProfessionalTax(),
                payslip.getTds(),
                payslip.getOtherDeductions(),
                payslip.getTotalDeductions(),
                payslip.getNetSalary(),
                payslip.getDeductionsJson(),
                payslip.getPdfS3Key(),
                payslip.getWorkingDays(),
                payslip.getPresentDays(),
                payslip.getLossOfPayDays(),
                payslip.getCreatedAt()
        );
    }
}
