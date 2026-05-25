package com.nexushr.ai.repository;

import com.nexushr.ai.entity.AiAttritionScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiAttritionScoreRepository extends JpaRepository<AiAttritionScore, Long> {
    List<AiAttritionScore> findByEmployeeIdOrderByComputedAtDesc(Long employeeId);

    @Query("SELECT s FROM AiAttritionScore s WHERE s.computedAt = (SELECT MAX(sub.computedAt) FROM AiAttritionScore sub WHERE sub.employee.id = s.employee.id)")
    List<AiAttritionScore> findLatestScoresForAllEmployees();

    Optional<AiAttritionScore> findFirstByEmployeeIdOrderByComputedAtDesc(Long employeeId);
}
