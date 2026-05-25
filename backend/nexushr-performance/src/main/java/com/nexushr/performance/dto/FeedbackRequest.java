package com.nexushr.performance.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class FeedbackRequest {
    private Long reviewCycleId;
    private Long fromEmployeeId;
    private Long toEmployeeId;
    private BigDecimal rating;
    private String strengths;
    private String improvementAreas;
    private String comments;
    private boolean anonymous;
}
