package com.nexushr.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexushr.ai.entity.AiAttritionScore;
import com.nexushr.ai.repository.AiAttritionScoreRepository;
import com.nexushr.ai.dto.AttritionPredictionResponse;
import com.nexushr.common.exception.ResourceNotFoundException;
import com.nexushr.employee.entity.Employee;
import com.nexushr.employee.repository.EmployeeRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttritionPredictorService {

    private final AiAttritionScoreRepository aiAttritionScoreRepository;
    private final EmployeeRepository employeeRepository;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void computeAndSaveAllEmployeeScores() {
        List<Employee> employees = employeeRepository.findAll().stream()
                .filter(e -> e.getStatus() == com.nexushr.common.enums.EmployeeStatus.ACTIVE)
                .collect(Collectors.toList());

        for (Employee employee : employees) {
            try {
                computeAndSaveScoreForEmployee(employee);
            } catch (Exception e) {
                log.error("Failed to compute attrition score for employee: {}", employee.getId(), e);
            }
        }
    }

    @Transactional
    public AttritionPredictionResponse recomputeForEmployee(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));
        AiAttritionScore score = computeAndSaveScoreForEmployee(employee);
        return mapToResponse(score);
    }

    public List<AttritionPredictionResponse> getLatestScores() {
        return aiAttritionScoreRepository.findLatestScoresForAllEmployees().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private AiAttritionScore computeAndSaveScoreForEmployee(Employee employee) {
        double scoreVal = 0.05; // Base risk
        Map<String, Object> features = new HashMap<>();

        // 1. Tenure analysis
        long daysOfTenure = ChronoUnit.DAYS.between(employee.getHireDate(), LocalDate.now());
        double tenureYears = daysOfTenure / 365.0;
        features.put("tenure_years", tenureYears);
        if (tenureYears < 1.0) {
            scoreVal += 0.15;
            features.put("tenure_risk", true);
        } else {
            features.put("tenure_risk", false);
        }

        // 2. Performance score analysis
        BigDecimal latestPerformanceScore = null;
        boolean performanceDrop = false;
        try {
            List<?> finalScores = entityManager.createNativeQuery(
                    "SELECT final_score FROM performance_reviews WHERE employee_id = :empId AND status = 'SUBMITTED' OR status = 'ACKNOWLEDGED' ORDER BY submitted_at DESC")
                    .setParameter("empId", employee.getId())
                    .setMaxResults(2)
                    .getResultList();

            if (!finalScores.isEmpty()) {
                latestPerformanceScore = (BigDecimal) finalScores.get(0);
                features.put("latest_performance_score", latestPerformanceScore);
                if (latestPerformanceScore.doubleValue() < 3.0) {
                    scoreVal += 0.25;
                    features.put("performance_risk", true);
                } else {
                    features.put("performance_risk", false);
                }

                if (finalScores.size() > 1) {
                    BigDecimal previousScore = (BigDecimal) finalScores.get(1);
                    if (latestPerformanceScore.compareTo(previousScore) < 0) {
                        scoreVal += 0.15;
                        performanceDrop = true;
                    }
                }
            } else {
                features.put("latest_performance_score", 3.0); // baseline
                features.put("performance_risk", false);
            }
        } catch (Exception e) {
            log.warn("Could not query performance reviews for employee {}", employee.getId(), e);
        }
        features.put("performance_drop", performanceDrop);

        // 3. Salary deviation (percentile in department)
        double percentile = 50.0;
        if (employee.getDepartment() != null && employee.getCtc() != null) {
            try {
                List<BigDecimal> ctcs = entityManager.createQuery(
                        "SELECT e.ctc FROM Employee e WHERE e.department.id = :deptId AND e.ctc IS NOT NULL", BigDecimal.class)
                        .setParameter("deptId", employee.getDepartment().getId())
                        .getResultList();

                if (ctcs.size() > 1) {
                    ctcs.sort(BigDecimal::compareTo);
                    int rank = ctcs.indexOf(employee.getCtc());
                    percentile = (rank / (double) (ctcs.size() - 1)) * 100;
                    features.put("salary_percentile", Math.round(percentile));
                    if (percentile <= 25.0) {
                        scoreVal += 0.20;
                        features.put("low_salary_risk", true);
                    } else {
                        features.put("low_salary_risk", false);
                    }
                } else {
                    features.put("salary_percentile", 100);
                    features.put("low_salary_risk", false);
                }
            } catch (Exception e) {
                log.warn("Could not calculate salary percentile for employee {}", employee.getId(), e);
            }
        } else {
            features.put("salary_percentile", 50.0);
            features.put("low_salary_risk", false);
        }

        // 4. Leave history analysis
        double leaveDaysCount = 0.0;
        try {
            BigDecimal totalLeaves = (BigDecimal) entityManager.createNativeQuery(
                    "SELECT COALESCE(SUM(total_days), 0) FROM leave_requests WHERE employee_id = :empId AND status = 'APPROVED' AND from_date >= :sixMonthsAgo")
                    .setParameter("empId", employee.getId())
                    .setParameter("sixMonthsAgo", LocalDate.now().minusMonths(6))
                    .getSingleResult();

            leaveDaysCount = totalLeaves.doubleValue();
            features.put("leave_days_6m", leaveDaysCount);
            if (leaveDaysCount > 15.0) {
                scoreVal += 0.15;
                features.put("excessive_leave_risk", true);
            } else {
                features.put("excessive_leave_risk", false);
            }
        } catch (Exception e) {
            log.warn("Could not calculate leave days for employee {}", employee.getId(), e);
        }

        // 5. Manager change or reporting lines
        if (employee.getManager() == null) {
            scoreVal += 0.10;
            features.put("no_manager_risk", true);
        } else {
            features.put("no_manager_risk", false);
        }

        // Final normalization and mapping
        double finalScore = Math.min(1.0, Math.max(0.0, scoreVal));
        String riskLevel;
        if (finalScore < 0.35) {
            riskLevel = "LOW";
        } else if (finalScore < 0.60) {
            riskLevel = "MEDIUM";
        } else if (finalScore < 0.80) {
            riskLevel = "HIGH";
        } else {
            riskLevel = "CRITICAL";
        }

        String featuresJsonStr = "{}";
        try {
            featuresJsonStr = objectMapper.writeValueAsString(features);
        } catch (Exception e) {
            log.error("Failed to serialize features map", e);
        }

        AiAttritionScore score = AiAttritionScore.builder()
                .employee(employee)
                .score(BigDecimal.valueOf(finalScore).setScale(4, RoundingMode.HALF_UP))
                .riskLevel(riskLevel)
                .computedAt(LocalDateTime.now())
                .featuresJson(featuresJsonStr)
                .modelVersion("v1.0-heuristic")
                .build();

        return aiAttritionScoreRepository.save(score);
    }

    private AttritionPredictionResponse mapToResponse(AiAttritionScore score) {
        Map<String, Object> features = new HashMap<>();
        if (score.getFeaturesJson() != null) {
            try {
                features = objectMapper.readValue(score.getFeaturesJson(), Map.class);
            } catch (Exception e) {
                log.error("Failed to parse features JSON", e);
            }
        }

        return AttritionPredictionResponse.builder()
                .id(score.getId())
                .employeeId(score.getEmployee().getId())
                .employeeName(score.getEmployee().getFullName())
                .departmentName(score.getEmployee().getDepartment() != null ? score.getEmployee().getDepartment().getName() : "N/A")
                .score(score.getScore())
                .riskLevel(score.getRiskLevel())
                .computedAt(score.getComputedAt())
                .features(features)
                .modelVersion(score.getModelVersion())
                .build();
    }
}
