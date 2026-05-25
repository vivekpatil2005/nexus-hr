package com.nexushr.performance.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class NormalizationPreviewResponse {
    private Long reviewId;
    private Long employeeId;
    private String employeeName;
    private String departmentName;
    private BigDecimal finalScore;
    private String currentBand;
    private String suggestedBand;
}
