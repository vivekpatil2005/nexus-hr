package com.nexushr.ai.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class SkillGapResponse {
    private Long employeeId;
    private String employeeName;
    private String designation;
    private Map<String, Integer> currentSkills;
    private Map<String, Integer> requiredSkills;
    private Map<String, Integer> gaps;
    private List<String> recommendations;
}
