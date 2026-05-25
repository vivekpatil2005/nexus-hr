package com.nexushr.performance.dto;

import com.nexushr.performance.entity.GoalStatus;
import com.nexushr.performance.entity.GoalType;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class GoalRequest {
    private Long employeeId;
    private Long reviewCycleId;
    private Long parentGoalId;
    private String title;
    private String description;
    private GoalType type;
    private String category;
    private BigDecimal targetValue;
    private BigDecimal currentValue;
    private BigDecimal weight;
    private String unit;
    private LocalDate startDate;
    private LocalDate dueDate;
    private GoalStatus status;
    private Integer progress;
}
