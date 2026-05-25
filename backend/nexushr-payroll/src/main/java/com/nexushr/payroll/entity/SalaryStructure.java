package com.nexushr.payroll.entity;

import com.nexushr.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "salary_structures")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryStructure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal basic;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal hra;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal da;

    @Column(name = "special_allowance", nullable = false, precision = 12, scale = 2)
    private BigDecimal specialAllowance;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal conveyance;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal medical;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal lta;

    @Column(name = "other_allowances", nullable = false, precision = 12, scale = 2)
    private BigDecimal otherAllowances;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal gross;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal ctc;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (hra == null) hra = BigDecimal.ZERO;
        if (da == null) da = BigDecimal.ZERO;
        if (specialAllowance == null) specialAllowance = BigDecimal.ZERO;
        if (conveyance == null) conveyance = BigDecimal.ZERO;
        if (medical == null) medical = BigDecimal.ZERO;
        if (lta == null) lta = BigDecimal.ZERO;
        if (otherAllowances == null) otherAllowances = BigDecimal.ZERO;
    }
}
