package com.nexushr.ai.service;

import com.nexushr.ai.dto.SkillGapResponse;
import com.nexushr.employee.entity.Employee;
import com.nexushr.employee.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SkillGapAnalysisServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private SkillGapAnalysisService skillGapAnalysisService;

    private Employee employee;

    @BeforeEach
    public void setUp() {
        employee = new Employee();
        employee.setId(1L);
        employee.setFirstName("Jane");
        employee.setLastName("Smith");
    }

    @Test
    public void testAnalyzeGap_DeveloperRoleWithGaps() {
        // Arrange
        employee.setDesignation("Software Engineer");
        Map<String, Object> skills = new HashMap<>();
        skills.put("Java", 2);
        skills.put("Spring Boot", "1"); // Testing string parsing support
        skills.put("SQL", 3);
        // Git and Communication are missing (0 level)
        employee.setSkills(skills);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        // Act
        SkillGapResponse response = skillGapAnalysisService.analyzeGap(1L);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getEmployeeId());
        assertEquals("Jane Smith", response.getEmployeeName());
        assertEquals("Software Engineer", response.getDesignation());

        // Check current skills
        assertEquals(2, response.getCurrentSkills().get("Java"));
        assertEquals(1, response.getCurrentSkills().get("Spring Boot"));
        assertEquals(3, response.getCurrentSkills().get("SQL"));
        assertEquals(0, response.getCurrentSkills().getOrDefault("Git", 0));

        // Check required skills for Developer/Engineer
        // Java: 3, Spring Boot: 3, SQL: 3, Git: 3, Communication: 3
        assertEquals(3, response.getRequiredSkills().get("Java"));
        assertEquals(3, response.getRequiredSkills().get("Spring Boot"));
        assertEquals(3, response.getRequiredSkills().get("SQL"));
        assertEquals(3, response.getRequiredSkills().get("Git"));

        // Check gaps
        // Java gap: 3 - 2 = 1
        assertEquals(1, response.getGaps().get("Java"));
        // Spring Boot gap: 3 - 1 = 2
        assertEquals(2, response.getGaps().get("Spring Boot"));
        // SQL gap: 3 - 3 = 0
        assertEquals(0, response.getGaps().get("SQL"));
        // Git gap: 3 - 0 = 3
        assertEquals(3, response.getGaps().get("Git"));

        // Recommendations should contain items for Java, Spring Boot, Git, Communication (which are missing/low)
        assertFalse(response.getRecommendations().isEmpty());
        assertTrue(response.getRecommendations().stream().anyMatch(r -> r.contains("Java")));
        assertTrue(response.getRecommendations().stream().anyMatch(r -> r.contains("Spring Boot")));
        assertTrue(response.getRecommendations().stream().anyMatch(r -> r.contains("Git")));
    }

    @Test
    public void testAnalyzeGap_HighlySkilledLeadNoGaps() {
        // Arrange
        employee.setDesignation("Lead Architect");
        Map<String, Object> skills = new HashMap<>();
        skills.put("Java", 5);
        skills.put("Spring Boot", 5);
        skills.put("System Design", 5);
        skills.put("SQL", 4);
        skills.put("Cloud Infrastructure", 4);
        skills.put("Leadership", 5);
        employee.setSkills(skills);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        // Act
        SkillGapResponse response = skillGapAnalysisService.analyzeGap(1L);

        // Assert
        assertNotNull(response);
        // Check gaps are all 0
        response.getGaps().values().forEach(gap -> assertEquals(0, gap));
        
        // Since there are no gaps, there should be default recommendations
        assertFalse(response.getRecommendations().isEmpty());
        assertTrue(response.getRecommendations().stream().anyMatch(r -> r.contains("mentoring junior developers")));
        assertTrue(response.getRecommendations().stream().anyMatch(r -> r.contains("Advanced certifications")));
    }
}
