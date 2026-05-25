package com.nexushr.performance.dto;

import com.nexushr.performance.entity.CycleStatus;
import com.nexushr.performance.entity.CycleType;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class ReviewCycleResponse {
    private Long id;
    private String name;
    private CycleType type;
    private LocalDate startDate;
    private LocalDate endDate;
    private CycleStatus status;
    private String description;
}
