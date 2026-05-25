package com.nexushr.performance.repository;

import com.nexushr.performance.entity.Goal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GoalRepository extends JpaRepository<Goal, Long> {
    List<Goal> findByEmployeeId(Long employeeId);
    List<Goal> findByEmployeeIdAndReviewCycleId(Long employeeId, Long reviewCycleId);
}
