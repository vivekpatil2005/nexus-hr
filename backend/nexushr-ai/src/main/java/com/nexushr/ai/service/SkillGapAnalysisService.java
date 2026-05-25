package com.nexushr.ai.service;

import com.nexushr.ai.dto.SkillGapResponse;
import com.nexushr.common.exception.ResourceNotFoundException;
import com.nexushr.employee.entity.Employee;
import com.nexushr.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SkillGapAnalysisService {

    private final EmployeeRepository employeeRepository;

    public SkillGapResponse analyzeGap(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));

        String designation = employee.getDesignation() != null ? employee.getDesignation() : "Associate";
        Map<String, Object> rawSkills = employee.getSkills();
        Map<String, Integer> currentSkills = new HashMap<>();

        if (rawSkills != null) {
            for (Map.Entry<String, Object> entry : rawSkills.entrySet()) {
                if (entry.getValue() instanceof Number) {
                    currentSkills.put(entry.getKey(), ((Number) entry.getValue()).intValue());
                } else if (entry.getValue() instanceof String) {
                    try {
                        currentSkills.put(entry.getKey(), Integer.parseInt((String) entry.getValue()));
                    } catch (NumberFormatException e) {
                        currentSkills.put(entry.getKey(), 1); // fallback
                    }
                }
            }
        }

        // Define expected skills based on designation
        Map<String, Integer> requiredSkills = getRequiredSkills(designation);
        Map<String, Integer> gaps = new HashMap<>();
        List<String> recommendations = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : requiredSkills.entrySet()) {
            String skill = entry.getKey();
            Integer reqLevel = entry.getValue();
            Integer curLevel = currentSkills.getOrDefault(skill, 0);
            
            int gap = reqLevel - curLevel;
            gaps.put(skill, Math.max(0, gap));

            if (gap > 0) {
                recommendations.add(getRecommendation(skill, gap));
            }
        }

        // Add default recommendations if employee is already highly skilled
        if (recommendations.isEmpty()) {
            recommendations.add("Consider mentoring junior developers in: " + String.join(", ", requiredSkills.keySet()));
            recommendations.add("Advanced certifications in enterprise Cloud Architecture / DevOps tools");
        }

        return SkillGapResponse.builder()
                .employeeId(employee.getId())
                .employeeName(employee.getFullName())
                .designation(designation)
                .currentSkills(currentSkills)
                .requiredSkills(requiredSkills)
                .gaps(gaps)
                .recommendations(recommendations)
                .build();
    }

    private Map<String, Integer> getRequiredSkills(String designation) {
        Map<String, Integer> reqs = new LinkedHashMap<>();
        String upperDesg = designation.toUpperCase();

        if (upperDesg.contains("LEAD") || upperDesg.contains("PRINCIPAL") || upperDesg.contains("ARCHITECT")) {
            reqs.put("Java", 5);
            reqs.put("Spring Boot", 5);
            reqs.put("System Design", 5);
            reqs.put("SQL", 4);
            reqs.put("Cloud Infrastructure", 4);
            reqs.put("Leadership", 4);
        } else if (upperDesg.contains("SENIOR") || upperDesg.contains("SR.")) {
            reqs.put("Java", 4);
            reqs.put("Spring Boot", 4);
            reqs.put("SQL", 4);
            reqs.put("System Design", 3);
            reqs.put("Cloud Infrastructure", 3);
            reqs.put("Communication", 4);
        } else if (upperDesg.contains("MANAGER") || upperDesg.contains("DIRECTOR") || upperDesg.contains("HEAD")) {
            reqs.put("Leadership", 5);
            reqs.put("Management", 5);
            reqs.put("Communication", 5);
            reqs.put("Strategic Planning", 4);
            reqs.put("Agile Delivery", 4);
        } else if (upperDesg.contains("DEVELOPER") || upperDesg.contains("ENGINEER") || upperDesg.contains("PROGRAMMER")) {
            reqs.put("Java", 3);
            reqs.put("Spring Boot", 3);
            reqs.put("SQL", 3);
            reqs.put("Git", 3);
            reqs.put("Communication", 3);
        } else {
            // General Office / Professional roles
            reqs.put("Communication", 3);
            reqs.put("Teamwork", 3);
            reqs.put("Problem Solving", 3);
            reqs.put("Office Productivity", 4);
        }
        return reqs;
    }

    private String getRecommendation(String skill, int gap) {
        String levelPrefix = gap >= 3 ? "Introductory " : "Intermediate/Advanced ";
        switch (skill) {
            case "Java":
                return levelPrefix + "Java Concepts & Concurrency course on Udemy or Coursera.";
            case "Spring Boot":
                return levelPrefix + "Spring Boot & Cloud Microservices Certification.";
            case "System Design":
                return "System Design Primer & High-Scale Systems course.";
            case "SQL":
                return "Database Query Optimization & Performance Tuning module.";
            case "Cloud Infrastructure":
                return "AWS or Azure Developer/Architect Associate Certification training.";
            case "Leadership":
                return "Enterprise Leadership & Strategic Management masterclass.";
            case "Management":
                return "Agile Project Management and Scrum Master Certification.";
            case "Communication":
                return "Professional Presentation & Team Communication training program.";
            case "Git":
                return "Advanced Git Workflows & Collaborative Coding course.";
            default:
                return "Targeted learning modules to improve competency in: " + skill;
        }
    }
}
