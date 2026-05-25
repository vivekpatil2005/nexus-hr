package com.nexushr.performance.dto;

import com.nexushr.performance.entity.ReviewStatus;
import com.nexushr.performance.entity.ReviewType;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ReviewRequest {
    private Long employeeId;
    private Long reviewerId;
    private Long cycleId;
    private ReviewType reviewType;
    private BigDecimal goalScore;
    private BigDecimal competencyScore;
    private BigDecimal peerScore;
    private BigDecimal managerScore;
    private BigDecimal finalScore;
    private String band;
    private String strengths;
    private String improvementAreas;
    private String managerComments;
    private String employeeComments;
    private ReviewStatus status;
}
