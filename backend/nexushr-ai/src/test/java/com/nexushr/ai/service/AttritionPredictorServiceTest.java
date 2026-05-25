package com.nexushr.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexushr.ai.entity.AiAttritionScore;
import com.nexushr.ai.repository.AiAttritionScoreRepository;
import com.nexushr.ai.dto.AttritionPredictionResponse;
import com.nexushr.employee.entity.Department;
import com.nexushr.employee.entity.Employee;
import com.nexushr.employee.repository.EmployeeRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AttritionPredictorServiceTest {

    @Mock
    private AiAttritionScoreRepository aiAttritionScoreRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private AttritionPredictorService attritionPredictorService;

    @Mock
    private Query nativeQueryMock;

    @Mock
    private TypedQuery<BigDecimal> typedQueryMock;

    @BeforeEach
    public void setUp() throws Exception {
        // Manually inject the mocked entityManager because it's annotated with @PersistenceContext and not part of Lombok's constructor injection
        ReflectionTestUtils.setField(attritionPredictorService, "entityManager", entityManager);

        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        
        // Mock default query behavior
        when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQueryMock);
        when(nativeQueryMock.setParameter(anyString(), any())).thenReturn(nativeQueryMock);
        when(nativeQueryMock.setMaxResults(anyInt())).thenReturn(nativeQueryMock);
        
        when(entityManager.createQuery(anyString(), eq(BigDecimal.class))).thenReturn(typedQueryMock);
        when(typedQueryMock.setParameter(anyString(), any())).thenReturn(typedQueryMock);
    }

    @Test
    public void testRecomputeForEmployee_BaselineLowRisk() {
        // Arrange
        Employee manager = new Employee();
        manager.setId(10L);
        manager.setFirstName("Jane");
        manager.setLastName("Doe");

        Department department = new Department();
        department.setId(1L);
        department.setName("Engineering");

        Employee employee = new Employee();
        employee.setId(2L);
        employee.setFirstName("John");
        employee.setLastName("Smith");
        employee.setHireDate(LocalDate.now().minusYears(3)); // 3 years tenure
        employee.setManager(manager);
        employee.setDepartment(department);
        employee.setCtc(BigDecimal.valueOf(100000));
        employee.setStatus(com.nexushr.common.enums.EmployeeStatus.ACTIVE);

        when(employeeRepository.findById(2L)).thenReturn(Optional.of(employee));
        
        // Mock performance reviews query (return empty list -> defaults to 3.0, no risk)
        when(nativeQueryMock.getResultList()).thenReturn(Collections.emptyList());
        
        // Mock salary deviation (return list where employee is at 100th percentile)
        when(typedQueryMock.getResultList()).thenReturn(Arrays.asList(BigDecimal.valueOf(100000)));

        // Mock leave requests (return 0 approved leaves)
        when(nativeQueryMock.getSingleResult()).thenReturn(BigDecimal.ZERO);

        // Mock save
        when(aiAttritionScoreRepository.save(any(AiAttritionScore.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AttritionPredictionResponse response = attritionPredictorService.recomputeForEmployee(2L);

        // Assert
        assertNotNull(response);
        assertEquals("LOW", response.getRiskLevel());
        assertTrue(response.getScore().doubleValue() < 0.35);
        verify(aiAttritionScoreRepository, times(1)).save(any(AiAttritionScore.class));
    }

    @Test
    public void testRecomputeForEmployee_HighRiskFactors() {
        // Arrange
        Department department = new Department();
        department.setId(1L);
        department.setName("Engineering");

        Employee employee = new Employee();
        employee.setId(3L);
        employee.setFirstName("Risky");
        employee.setLastName("Employee");
        employee.setHireDate(LocalDate.now().minusMonths(6)); // < 1 year tenure -> risk +0.15
        employee.setManager(null); // No manager -> risk +0.10
        employee.setDepartment(department);
        employee.setCtc(BigDecimal.valueOf(30000));
        employee.setStatus(com.nexushr.common.enums.EmployeeStatus.ACTIVE);

        when(employeeRepository.findById(3L)).thenReturn(Optional.of(employee));

        // Mock performance reviews query (return 2.5 score -> risk +0.25)
        when(nativeQueryMock.getResultList()).thenReturn(Arrays.asList(BigDecimal.valueOf(2.5)));

        // Mock salary deviation (ctcs contains 30k, 50k, 80k, 100k -> 30k is index 0 out of 3, i.e., 0th percentile -> risk +0.20)
        when(typedQueryMock.getResultList()).thenReturn(Arrays.asList(
                BigDecimal.valueOf(30000), BigDecimal.valueOf(50000), BigDecimal.valueOf(80000), BigDecimal.valueOf(100000)
        ));

        // Mock leave requests (return 20 approved leaves -> risk +0.15)
        when(nativeQueryMock.getSingleResult()).thenReturn(BigDecimal.valueOf(20));

        // Mock save
        when(aiAttritionScoreRepository.save(any(AiAttritionScore.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AttritionPredictionResponse response = attritionPredictorService.recomputeForEmployee(3L);

        // Assert
        assertNotNull(response);
        // Base score = 0.05
        // + 0.15 (tenure < 1 yr)
        // + 0.25 (perf < 3.0)
        // + 0.20 (low salary)
        // + 0.15 (leaves > 15)
        // + 0.10 (no manager)
        // Total score = 0.90 -> CRITICAL risk level
        assertEquals("CRITICAL", response.getRiskLevel());
        assertTrue(response.getScore().doubleValue() >= 0.80);
        verify(aiAttritionScoreRepository, times(1)).save(any(AiAttritionScore.class));
    }
}
