package com.nexushr.performance.controller;

import com.nexushr.common.dto.ApiResponse;
import com.nexushr.performance.dto.GoalRequest;
import com.nexushr.performance.dto.GoalResponse;
import com.nexushr.performance.service.GoalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/goals")
@Tag(name = "Goals", description = "Performance goals management")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;

    @PostMapping
    @Operation(summary = "Create a new goal")
    public ResponseEntity<ApiResponse<GoalResponse>> createGoal(@Valid @RequestBody GoalRequest request) {
        GoalResponse response = goalService.createGoal(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Goal created successfully", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing goal")
    public ResponseEntity<ApiResponse<GoalResponse>> updateGoal(@PathVariable Long id, @RequestBody GoalRequest request) {
        GoalResponse response = goalService.updateGoal(id, request);
        return ResponseEntity.ok(ApiResponse.success("Goal updated successfully", response));
    }

    @GetMapping("/employee/{empId}")
    @Operation(summary = "Get all goals for an employee")
    public ResponseEntity<ApiResponse<List<GoalResponse>>> getGoalsByEmployee(
            @PathVariable Long empId,
            @RequestParam(required = false) Long cycleId) {
        List<GoalResponse> goals;
        if (cycleId != null) {
            goals = goalService.getGoalsByEmployeeAndCycle(empId, cycleId);
        } else {
            goals = goalService.getGoalsByEmployee(empId);
        }
        return ResponseEntity.ok(ApiResponse.success("Goals retrieved successfully", goals));
    }
}
