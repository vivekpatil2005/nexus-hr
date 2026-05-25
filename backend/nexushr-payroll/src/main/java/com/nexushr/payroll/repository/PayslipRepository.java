package com.nexushr.payroll.repository;

import com.nexushr.payroll.entity.Payslip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayslipRepository extends JpaRepository<Payslip, Long> {
    List<Payslip> findByEmployeeIdOrderByPayrollRunPeriodYearDescPayrollRunPeriodMonthDesc(Long employeeId);
    List<Payslip> findByPayrollRunId(Long payrollRunId);
    Optional<Payslip> findByEmployeeIdAndPayrollRunPeriodMonthAndPayrollRunPeriodYear(Long employeeId, int month, int year);
}
