package com.nexushr.performance.repository;

import com.nexushr.performance.entity.PerformanceReview;
import com.nexushr.performance.entity.ReviewType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PerformanceReviewRepository extends JpaRepository<PerformanceReview, Long> {
    List<PerformanceReview> findByEmployeeId(Long employeeId);
    List<PerformanceReview> findByEmployeeIdAndCycleId(Long employeeId, Long cycleId);
    Optional<PerformanceReview> findByEmployeeIdAndCycleIdAndReviewTypeAndReviewerId(Long employeeId, Long cycleId, ReviewType reviewType, Long reviewerId);
    List<PerformanceReview> findByCycleId(Long cycleId);
}
