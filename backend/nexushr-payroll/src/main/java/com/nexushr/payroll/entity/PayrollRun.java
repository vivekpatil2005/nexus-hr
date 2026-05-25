package com.nexushr.payroll.entity;

import com.nexushr.auth.entity.User;
import com.nexushr.common.enums.PayrollRunStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payroll_runs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "period_month", nullable = false)
    private int periodMonth;

    @Column(name = "period_year", nullable = false)
    private int periodYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PayrollRunStatus status;

    @Column(name = "total_gross", precision = 14, scale = 2)
    private BigDecimal totalGross;

    @Column(name = "total_deductions", precision = 14, scale = 2)
    private BigDecimal totalDeductions;

    @Column(name = "total_net", precision = 14, scale = 2)
    private BigDecimal totalNet;

    @Column(name = "employee_count")
    private int employeeCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_by")
    private User runBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (status == null) {
            status = PayrollRunStatus.DRAFT;
        }
        if (totalGross == null) totalGross = BigDecimal.ZERO;
        if (totalDeductions == null) totalDeductions = BigDecimal.ZERO;
        if (totalNet == null) totalNet = BigDecimal.ZERO;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
