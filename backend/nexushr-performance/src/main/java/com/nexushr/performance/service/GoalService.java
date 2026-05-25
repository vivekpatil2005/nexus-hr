package com.nexushr.performance.service;

import com.nexushr.common.exception.ResourceNotFoundException;
import com.nexushr.employee.entity.Employee;
import com.nexushr.employee.repository.EmployeeRepository;
import com.nexushr.performance.dto.GoalRequest;
import com.nexushr.performance.dto.GoalResponse;
import com.nexushr.performance.entity.Goal;
import com.nexushr.performance.entity.GoalStatus;
import com.nexushr.performance.entity.ReviewCycle;
import com.nexushr.performance.repository.GoalRepository;
import com.nexushr.performance.repository.ReviewCycleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GoalService {

    private final GoalRepository goalRepository;
    private final EmployeeRepository employeeRepository;
    private final ReviewCycleRepository reviewCycleRepository;

    @Transactional
    public GoalResponse createGoal(GoalRequest request) {
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", request.getEmployeeId()));

        ReviewCycle cycle = null;
        if (request.getReviewCycleId() != null) {
            cycle = reviewCycleRepository.findById(request.getReviewCycleId())
                    .orElseThrow(() -> new ResourceNotFoundException("ReviewCycle", "id", request.getReviewCycleId()));
        }

        Goal parentGoal = null;
        if (request.getParentGoalId() != null) {
            parentGoal = goalRepository.findById(request.getParentGoalId())
                    .orElseThrow(() -> new ResourceNotFoundException("ParentGoal", "id", request.getParentGoalId()));
        }

        Goal goal = Goal.builder()
                .employee(employee)
                .reviewCycle(cycle)
                .parentGoal(parentGoal)
                .title(request.getTitle())
                .description(request.getDescription())
                .type(request.getType())
                .category(request.getCategory())
                .targetValue(request.getTargetValue())
                .currentValue(request.getCurrentValue() != null ? request.getCurrentValue() : BigDecimal.ZERO)
                .weight(request.getWeight() != null ? request.getWeight() : BigDecimal.ONE)
                .unit(request.getUnit())
                .startDate(request.getStartDate())
                .dueDate(request.getDueDate())
                .status(request.getStatus() != null ? request.getStatus() : GoalStatus.NOT_STARTED)
                .progress(request.getProgress() != null ? request.getProgress() : 0)
                .build();

        Goal savedGoal = goalRepository.save(goal);
        return mapToResponse(savedGoal);
    }

    @Transactional
    public GoalResponse updateGoal(Long id, GoalRequest request) {
        Goal goal = goalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Goal", "id", id));

        if (request.getTitle() != null) goal.setTitle(request.getTitle());
        if (request.getDescription() != null) goal.setDescription(request.getDescription());
        if (request.getCategory() != null) goal.setCategory(request.getCategory());
        if (request.getTargetValue() != null) goal.setTargetValue(request.getTargetValue());
        if (request.getCurrentValue() != null) goal.setCurrentValue(request.getCurrentValue());
        if (request.getWeight() != null) goal.setWeight(request.getWeight());
        if (request.getUnit() != null) goal.setUnit(request.getUnit());
        if (request.getStartDate() != null) goal.setStartDate(request.getStartDate());
        if (request.getDueDate() != null) goal.setDueDate(request.getDueDate());
        if (request.getStatus() != null) goal.setStatus(request.getStatus());
        if (request.getProgress() != null) {
            goal.setProgress(request.getProgress());
            if (request.getProgress() >= 100) {
                goal.setStatus(GoalStatus.COMPLETED);
            } else if (request.getProgress() > 0 && goal.getStatus() == GoalStatus.NOT_STARTED) {
                goal.setStatus(GoalStatus.IN_PROGRESS);
            }
        }

        // Auto-calculate progress based on current/target values if both exist
        if (goal.getTargetValue() != null && goal.getCurrentValue() != null && goal.getTargetValue().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal progressRatio = goal.getCurrentValue().divide(goal.getTargetValue(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            int calculatedProgress = Math.min(100, Math.max(0, progressRatio.intValue()));
            goal.setProgress(calculatedProgress);
            if (calculatedProgress >= 100) {
                goal.setStatus(GoalStatus.COMPLETED);
            } else if (calculatedProgress > 0 && goal.getStatus() == GoalStatus.NOT_STARTED) {
                goal.setStatus(GoalStatus.IN_PROGRESS);
            }
        }

        if (request.getParentGoalId() != null) {
            Goal parentGoal = goalRepository.findById(request.getParentGoalId())
                    .orElseThrow(() -> new ResourceNotFoundException("ParentGoal", "id", request.getParentGoalId()));
            goal.setParentGoal(parentGoal);
        }

        Goal savedGoal = goalRepository.save(goal);
        return mapToResponse(savedGoal);
    }

    public List<GoalResponse> getGoalsByEmployee(Long employeeId) {
        return goalRepository.findByEmployeeId(employeeId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<GoalResponse> getGoalsByEmployeeAndCycle(Long employeeId, Long cycleId) {
        return goalRepository.findByEmployeeIdAndReviewCycleId(employeeId, cycleId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public GoalResponse mapToResponse(Goal goal) {
        return GoalResponse.builder()
                .id(goal.getId())
                .employeeId(goal.getEmployee().getId())
                .employeeName(goal.getEmployee().getFullName())
                .reviewCycleId(goal.getReviewCycle() != null ? goal.getReviewCycle().getId() : null)
                .reviewCycleName(goal.getReviewCycle() != null ? goal.getReviewCycle().getName() : null)
                .parentGoalId(goal.getParentGoal() != null ? goal.getParentGoal().getId() : null)
                .parentGoalTitle(goal.getParentGoal() != null ? goal.getParentGoal().getTitle() : null)
                .title(goal.getTitle())
                .description(goal.getDescription())
                .type(goal.getType())
                .category(goal.getCategory())
                .targetValue(goal.getTargetValue())
                .currentValue(goal.getCurrentValue())
                .weight(goal.getWeight())
                .unit(goal.getUnit())
                .startDate(goal.getStartDate())
                .dueDate(goal.getDueDate())
                .status(goal.getStatus())
                .progress(goal.getProgress())
                .build();
    }
}
