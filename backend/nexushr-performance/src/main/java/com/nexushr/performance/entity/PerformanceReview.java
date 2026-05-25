package com.nexushr.performance.entity;

import com.nexushr.common.entity.BaseEntity;
import com.nexushr.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "performance_reviews", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"employee_id", "cycle_id", "review_type", "reviewer_id"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceReview extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private Employee reviewer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycle_id", nullable = false)
    private ReviewCycle cycle;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_type", nullable = false)
    private ReviewType reviewType;

    @Column(name = "goal_score", precision = 4, scale = 2)
    private BigDecimal goalScore;

    @Column(name = "competency_score", precision = 4, scale = 2)
    private BigDecimal competencyScore;

    @Column(name = "peer_score", precision = 4, scale = 2)
    private BigDecimal peerScore;

    @Column(name = "manager_score", precision = 4, scale = 2)
    private BigDecimal managerScore;

    @Column(name = "final_score", precision = 4, scale = 2)
    private BigDecimal finalScore;

    @Column(length = 10)
    private String band;

    @Column(columnDefinition = "TEXT")
    private String strengths;

    @Column(name = "improvement_areas", columnDefinition = "TEXT")
    private String improvementAreas;

    @Column(name = "manager_comments", columnDefinition = "TEXT")
    private String managerComments;

    @Column(name = "employee_comments", columnDefinition = "TEXT")
    private String employeeComments;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewStatus status;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;
}
