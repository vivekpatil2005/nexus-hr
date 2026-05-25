package com.nexushr.performance.dto;

import com.nexushr.performance.entity.GoalStatus;
import com.nexushr.performance.entity.GoalType;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class GoalResponse {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private Long reviewCycleId;
    private String reviewCycleName;
    private Long parentGoalId;
    private String parentGoalTitle;
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
