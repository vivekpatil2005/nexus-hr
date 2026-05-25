package com.nexushr.ai.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class AttritionPredictionResponse {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private String departmentName;
    private BigDecimal score;
    private String riskLevel;
    private LocalDateTime computedAt;
    private Map<String, Object> features;
    private String modelVersion;
}
