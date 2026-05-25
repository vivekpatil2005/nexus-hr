package com.nexushr.payroll.batch;

import com.nexushr.attendance.entity.AttendanceRecord;
import com.nexushr.attendance.repository.AttendanceRecordRepository;
import com.nexushr.common.enums.AttendanceStatus;
import com.nexushr.employee.entity.Employee;
import com.nexushr.payroll.entity.PayrollRun;
import com.nexushr.payroll.entity.Payslip;
import com.nexushr.payroll.entity.SalaryStructure;
import com.nexushr.payroll.repository.PayrollRunRepository;
import com.nexushr.payroll.repository.SalaryStructureRepository;
import com.nexushr.payroll.service.PayrollCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
@StepScope
public class PayrollItemProcessor implements ItemProcessor<Employee, Payslip> {

    private static final Logger log = LoggerFactory.getLogger(PayrollItemProcessor.class);

    @Autowired
    private SalaryStructureRepository salaryStructureRepository;

    @Autowired
    private AttendanceRecordRepository attendanceRecordRepository;

    @Autowired
    private PayrollRunRepository payrollRunRepository;

    @Autowired
    private PayrollCalculator payrollCalculator;

    private final int month;
    private final int year;
    private final Long payrollRunId;

    public PayrollItemProcessor(
            @Value("#{jobParameters['month']}") Integer month,
            @Value("#{jobParameters['year']}") Integer year,
            @Value("#{jobParameters['payrollRunId']}") Long payrollRunId) {
        this.month = month != null ? month : 1;
        this.year = year != null ? year : 2026;
        this.payrollRunId = payrollRunId;
    }

    @Override
    public Payslip process(Employee employee) {
        log.info("Processing payroll for employee: {} (ID: {})", employee.getFullName(), employee.getId());

        // 1. Fetch active salary structure
        SalaryStructure structure = salaryStructureRepository.findByEmployeeIdAndActiveTrue(employee.getId())
                .orElse(null);

        if (structure == null) {
            log.warn("No active salary structure found for employee: {}. Skipping from payroll run.", employee.getFullName());
            return null;
        }

        // 2. Fetch payroll run
        PayrollRun run = payrollRunRepository.findById(payrollRunId)
                .orElseThrow(() -> new IllegalArgumentException("Payroll run not found: " + payrollRunId));

        // 3. Count LOP days from AttendanceRecord
        int daysInMonth = LocalDate.of(year, month, 1).lengthOfMonth();
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = LocalDate.of(year, month, daysInMonth);

        List<AttendanceRecord> attendanceRecords = attendanceRecordRepository.findAllByEmployeeIdAndDateBetween(
                employee.getId(), startDate, endDate);

        BigDecimal lossOfPayDays = BigDecimal.ZERO;
        for (AttendanceRecord record : attendanceRecords) {
            if (record.getStatus() == AttendanceStatus.ABSENT) {
                lossOfPayDays = lossOfPayDays.add(BigDecimal.ONE);
            } else if (record.getStatus() == AttendanceStatus.HALF_DAY) {
                lossOfPayDays = lossOfPayDays.add(new BigDecimal("0.5"));
            }
        }

        // 4. Calculate salary components
        Payslip payslip = payrollCalculator.calculate(structure, month, year, lossOfPayDays);
        payslip.setPayrollRun(run);

        return payslip;
    }
}
