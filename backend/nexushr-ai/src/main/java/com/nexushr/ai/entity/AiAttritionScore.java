package com.nexushr.ai.entity;

import com.nexushr.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_attrition_scores")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiAttritionScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal score;

    @Column(name = "risk_level", nullable = false, length = 10)
    private String riskLevel;

    @Column(name = "computed_at", nullable = false)
    private LocalDateTime computedAt;

    @Column(name = "features_json", columnDefinition = "jsonb")
    private String featuresJson;

    @Column(name = "model_version", length = 20)
    private String modelVersion;

    @PrePersist
    public void prePersist() {
        if (computedAt == null) {
            computedAt = LocalDateTime.now();
        }
    }
}
