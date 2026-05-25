package com.nexushr.performance.repository;

import com.nexushr.performance.entity.PeerFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PeerFeedbackRepository extends JpaRepository<PeerFeedback, Long> {
    List<PeerFeedback> findByReviewCycleIdAndToEmployeeId(Long reviewCycleId, Long toEmployeeId);
    List<PeerFeedback> findByReviewCycleIdAndFromEmployeeId(Long reviewCycleId, Long fromEmployeeId);
}
