package com.nexushr.performance.dto;

import com.nexushr.performance.entity.CycleStatus;
import com.nexushr.performance.entity.CycleType;
import lombok.Data;
import java.time.LocalDate;

@Data
public class ReviewCycleRequest {
    private String name;
    private CycleType type;
    private LocalDate startDate;
    private LocalDate endDate;
    private CycleStatus status;
    private String description;
}
