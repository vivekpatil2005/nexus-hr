package com.nexushr.performance.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class FeedbackResponse {
    private Long id;
    private Long reviewCycleId;
    private String reviewCycleName;
    private Long fromEmployeeId;
    private String fromEmployeeName;
    private Long toEmployeeId;
    private String toEmployeeName;
    private BigDecimal rating;
    private String strengths;
    private String improvementAreas;
    private String comments;
    private boolean anonymous;
    private LocalDateTime submittedAt;
}
