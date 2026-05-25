package com.nexushr.performance.entity;

import com.nexushr.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "peer_feedbacks", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"review_cycle_id", "from_employee_id", "to_employee_id"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeerFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_cycle_id", nullable = false)
    private ReviewCycle reviewCycle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_employee_id", nullable = false)
    private Employee fromEmployee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_employee_id", nullable = false)
    private Employee toEmployee;

    @Column(precision = 3, scale = 1)
    private BigDecimal rating;

    @Column(columnDefinition = "TEXT")
    private String strengths;

    @Column(name = "improvement_areas", columnDefinition = "TEXT")
    private String improvementAreas;

    @Column(columnDefinition = "TEXT")
    private String comments;

    @Column(name = "is_anonymous", nullable = false)
    private boolean anonymous = true;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @PrePersist
    public void prePersist() {
        if (submittedAt == null) {
            submittedAt = LocalDateTime.now();
        }
    }
}
