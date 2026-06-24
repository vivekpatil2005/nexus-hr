package com.nexushr.performance.controller;

import com.nexushr.common.dto.ApiResponse;
import com.nexushr.common.exception.ResourceNotFoundException;
import com.nexushr.common.security.SecurityUtils;
import com.nexushr.employee.entity.Employee;
import com.nexushr.employee.repository.EmployeeRepository;
import com.nexushr.auth.entity.User;
import com.nexushr.auth.repository.UserRepository;
import com.nexushr.performance.dto.*;
import com.nexushr.performance.service.PerformanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/performance")
@Tag(name = "Performance", description = "Performance reviews, 360 feedback, and rating normalization")
@RequiredArgsConstructor
public class PerformanceController {

    private final PerformanceService performanceService;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    @GetMapping("/cycles")
    @Operation(summary = "Get all review cycles")
    public ResponseEntity<ApiResponse<List<ReviewCycleResponse>>> getAllCycles() {
        return ResponseEntity.ok(ApiResponse.success("Cycles retrieved successfully", performanceService.getAllCycles()));
    }

    @PostMapping("/cycles")
    @Operation(summary = "Create a review cycle")
    public ResponseEntity<ApiResponse<ReviewCycleResponse>> createCycle(@Valid @RequestBody ReviewCycleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Cycle created successfully", performanceService.createCycle(request)));
    }

    @GetMapping("/reviews/me")
    @Operation(summary = "Get reviews for the currently logged in employee")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getMyReviews() {
        String username = SecurityUtils.getCurrentUsername()
                .orElseThrow(() -> new ResourceNotFoundException("Logged in user details not found", "username", "current"));
        
        User user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        if (user.getEmployeeId() == null) {
            return ResponseEntity.ok(ApiResponse.success("No employee profile linked to user", Collections.emptyList()));
        }
        return ResponseEntity.ok(ApiResponse.success("Reviews retrieved successfully", performanceService.getReviewsByEmployee(user.getEmployeeId())));
    }

    @GetMapping("/reviews/employee/{empId}")
    @Operation(summary = "Get reviews for a specific employee")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getEmployeeReviews(@PathVariable Long empId) {
        return ResponseEntity.ok(ApiResponse.success("Reviews retrieved successfully", performanceService.getReviewsByEmployee(empId)));
    }

    @PostMapping("/reviews")
    @Operation(summary = "Submit a performance review (self, manager, or peer)")
    public ResponseEntity<ApiResponse<ReviewResponse>> submitReview(@Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Review submitted successfully", performanceService.submitReview(request)));
    }

    @PostMapping("/reviews/{id}/acknowledge")
    @Operation(summary = "Acknowledge a completed performance review")
    public ResponseEntity<ApiResponse<ReviewResponse>> acknowledgeReview(@PathVariable Long id, @RequestParam(required = false) String comments) {
        return ResponseEntity.ok(ApiResponse.success("Review acknowledged successfully", performanceService.acknowledgeReview(id, comments)));
    }

    @PostMapping("/feedbacks")
    @Operation(summary = "Submit peer feedback")
    public ResponseEntity<ApiResponse<FeedbackResponse>> submitPeerFeedback(@Valid @RequestBody FeedbackRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Peer feedback submitted successfully", performanceService.submitPeerFeedback(request)));
    }

    @GetMapping("/feedbacks/for/{empId}")
    @Operation(summary = "Get peer feedback submitted for an employee")
    public ResponseEntity<ApiResponse<List<FeedbackResponse>>> getFeedbackForEmployee(@RequestParam Long cycleId, @PathVariable Long empId) {
        return ResponseEntity.ok(ApiResponse.success("Feedback retrieved successfully", performanceService.getFeedbackForEmployee(cycleId, empId)));
    }

    @GetMapping("/feedbacks/from/{empId}")
    @Operation(summary = "Get peer feedback submitted by an employee")
    public ResponseEntity<ApiResponse<List<FeedbackResponse>>> getFeedbackFromEmployee(@RequestParam Long cycleId, @PathVariable Long empId) {
        return ResponseEntity.ok(ApiResponse.success("Feedback retrieved successfully", performanceService.getFeedbackFromEmployee(cycleId, empId)));
    }

    @GetMapping("/cycles/{cycleId}/normalize")
    @Operation(summary = "Preview bell-curve rating normalization for a cycle")
    public ResponseEntity<ApiResponse<List<NormalizationPreviewResponse>>> previewNormalization(@PathVariable Long cycleId) {
        return ResponseEntity.ok(ApiResponse.success("Normalization preview generated", performanceService.normalizeRatings(cycleId, false)));
    }

    @PostMapping("/cycles/{cycleId}/normalize")
    @Operation(summary = "Apply bell-curve rating normalization for a cycle")
    public ResponseEntity<ApiResponse<List<NormalizationPreviewResponse>>> applyNormalization(@PathVariable Long cycleId) {
        return ResponseEntity.ok(ApiResponse.success("Normalization applied successfully", performanceService.normalizeRatings(cycleId, true)));
    }
}
