package com.nexushr.payroll.entity;

import com.nexushr.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "payslips")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payslip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_run_id", nullable = false)
    private PayrollRun payrollRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "emp_code", nullable = false, length = 20)
    private String empCode;

    @Column(name = "employee_name", nullable = false, length = 100)
    private String employeeName;

    @Column(length = 100)
    private String department;

    @Column(length = 100)
    private String designation;

    // Earnings
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal basic;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal hra;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal da;

    @Column(name = "special_allowance", nullable = false, precision = 12, scale = 2)
    private BigDecimal specialAllowance;

    @Column(name = "other_earnings", nullable = false, precision = 12, scale = 2)
    private BigDecimal otherEarnings;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal gross;

    // Deductions
    @Column(name = "pf_employee", nullable = false, precision = 12, scale = 2)
    private BigDecimal pfEmployee;

    @Column(name = "pf_employer", nullable = false, precision = 12, scale = 2)
    private BigDecimal pfEmployer;

    @Column(name = "esi_employee", nullable = false, precision = 12, scale = 2)
    private BigDecimal esiEmployee;

    @Column(name = "esi_employer", nullable = false, precision = 12, scale = 2)
    private BigDecimal esiEmployer;

    @Column(name = "professional_tax", nullable = false, precision = 12, scale = 2)
    private BigDecimal professionalTax;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal tds;

    @Column(name = "other_deductions", nullable = false, precision = 12, scale = 2)
    private BigDecimal otherDeductions;

    @Column(name = "total_deductions", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalDeductions;

    // Net
    @Column(name = "net_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal netSalary;

    // Metadata
    @Column(name = "deductions_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> deductionsJson;

    @Column(name = "pdf_s3_key", length = 500)
    private String pdfS3Key;

    @Column(name = "working_days")
    private Integer workingDays;

    @Column(name = "present_days")
    private Integer presentDays;

    @Column(name = "loss_of_pay_days", precision = 5, scale = 1)
    private BigDecimal lossOfPayDays;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (basic == null) basic = BigDecimal.ZERO;
        if (hra == null) hra = BigDecimal.ZERO;
        if (da == null) da = BigDecimal.ZERO;
        if (specialAllowance == null) specialAllowance = BigDecimal.ZERO;
        if (otherEarnings == null) otherEarnings = BigDecimal.ZERO;
        if (gross == null) gross = BigDecimal.ZERO;
        if (pfEmployee == null) pfEmployee = BigDecimal.ZERO;
        if (pfEmployer == null) pfEmployer = BigDecimal.ZERO;
        if (esiEmployee == null) esiEmployee = BigDecimal.ZERO;
        if (esiEmployer == null) esiEmployer = BigDecimal.ZERO;
        if (professionalTax == null) professionalTax = BigDecimal.ZERO;
        if (tds == null) tds = BigDecimal.ZERO;
        if (otherDeductions == null) otherDeductions = BigDecimal.ZERO;
        if (totalDeductions == null) totalDeductions = BigDecimal.ZERO;
        if (netSalary == null) netSalary = BigDecimal.ZERO;
        if (lossOfPayDays == null) lossOfPayDays = BigDecimal.ZERO;
    }
}
