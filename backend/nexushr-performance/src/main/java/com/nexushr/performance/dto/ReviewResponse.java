package com.nexushr.performance.dto;

import com.nexushr.performance.entity.ReviewStatus;
import com.nexushr.performance.entity.ReviewType;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ReviewResponse {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private String employeeDesignation;
    private String employeeDepartment;
    private Long reviewerId;
    private String reviewerName;
    private Long cycleId;
    private String cycleName;
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
    private LocalDateTime submittedAt;
    private LocalDateTime acknowledgedAt;
}
