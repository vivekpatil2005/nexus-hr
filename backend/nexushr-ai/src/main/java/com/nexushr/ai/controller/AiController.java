package com.nexushr.ai.controller;

import com.nexushr.common.dto.ApiResponse;
import com.nexushr.ai.dto.AttritionPredictionResponse;
import com.nexushr.ai.dto.ChatRequest;
import com.nexushr.ai.dto.ChatResponse;
import com.nexushr.ai.dto.SkillGapResponse;
import com.nexushr.ai.service.AttritionPredictorService;
import com.nexushr.ai.service.HrChatbotService;
import com.nexushr.ai.service.SkillGapAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI Insights", description = "AI workforce intelligence, attrition predictions, skill gaps, and chatbot")
@RequiredArgsConstructor
public class AiController {

    private final AttritionPredictorService attritionPredictorService;
    private final SkillGapAnalysisService skillGapAnalysisService;
    private final HrChatbotService hrChatbotService;

    @GetMapping("/attrition")
    @Operation(summary = "Get the latest attrition predictions for all active employees")
    public ResponseEntity<ApiResponse<List<AttritionPredictionResponse>>> getLatestAttritionScores() {
        return ResponseEntity.ok(ApiResponse.success("Attrition scores retrieved successfully", attritionPredictorService.getLatestScores()));
    }

    @PostMapping("/attrition/compute")
    @Operation(summary = "Recompute attrition scores for all active employees")
    public ResponseEntity<ApiResponse<String>> computeAllAttritionScores() {
        attritionPredictorService.computeAndSaveAllEmployeeScores();
        return ResponseEntity.ok(ApiResponse.success("Attrition recomputation completed successfully", "Recomputation complete"));
    }

    @PostMapping("/attrition/compute/{employeeId}")
    @Operation(summary = "Recompute attrition score for a specific employee")
    public ResponseEntity<ApiResponse<AttritionPredictionResponse>> computeEmployeeAttritionScore(@PathVariable Long employeeId) {
        return ResponseEntity.ok(ApiResponse.success("Attrition score computed successfully", attritionPredictorService.recomputeForEmployee(employeeId)));
    }

    @GetMapping("/skills/gap/{employeeId}")
    @Operation(summary = "Analyze skill gaps and get recommendations for an employee")
    public ResponseEntity<ApiResponse<SkillGapResponse>> getSkillGapAnalysis(@PathVariable Long employeeId) {
        return ResponseEntity.ok(ApiResponse.success("Skill gap analysis retrieved successfully", skillGapAnalysisService.analyzeGap(employeeId)));
    }

    @PostMapping("/chat")
    @Operation(summary = "Interact with the HR Copilot Chatbot")
    public ResponseEntity<ApiResponse<ChatResponse>> chat(@Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Chat response generated successfully", hrChatbotService.chat(request.getMessage())));
    }
}
