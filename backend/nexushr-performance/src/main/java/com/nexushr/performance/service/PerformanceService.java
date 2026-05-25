package com.nexushr.performance.service;

import com.nexushr.common.exception.ResourceNotFoundException;
import com.nexushr.employee.entity.Employee;
import com.nexushr.employee.repository.EmployeeRepository;
import com.nexushr.performance.dto.*;
import com.nexushr.performance.entity.*;
import com.nexushr.performance.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PerformanceService {

    private final ReviewCycleRepository reviewCycleRepository;
    private final PerformanceReviewRepository performanceReviewRepository;
    private final PeerFeedbackRepository peerFeedbackRepository;
    private final EmployeeRepository employeeRepository;

    // Review Cycle operations
    public List<ReviewCycleResponse> getAllCycles() {
        return reviewCycleRepository.findAll().stream()
                .map(this::mapToCycleResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReviewCycleResponse createCycle(ReviewCycleRequest request) {
        ReviewCycle cycle = ReviewCycle.builder()
                .name(request.getName())
                .type(request.getType())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(request.getStatus() != null ? request.getStatus() : CycleStatus.UPCOMING)
                .description(request.getDescription())
                .build();
        return mapToCycleResponse(reviewCycleRepository.save(cycle));
    }

    @Transactional
    public ReviewCycleResponse updateCycleStatus(Long id, CycleStatus status) {
        ReviewCycle cycle = reviewCycleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ReviewCycle", "id", id));
        cycle.setStatus(status);
        return mapToCycleResponse(reviewCycleRepository.save(cycle));
    }

    // Performance Review operations
    public List<ReviewResponse> getReviewsByEmployee(Long employeeId) {
        return performanceReviewRepository.findByEmployeeId(employeeId).stream()
                .map(this::mapToReviewResponse)
                .collect(Collectors.toList());
    }

    public List<ReviewResponse> getReviewsByCycle(Long cycleId) {
        return performanceReviewRepository.findByCycleId(cycleId).stream()
                .map(this::mapToReviewResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReviewResponse submitReview(ReviewRequest request) {
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", request.getEmployeeId()));
        Employee reviewer = employeeRepository.findById(request.getReviewerId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", request.getReviewerId()));
        ReviewCycle cycle = reviewCycleRepository.findById(request.getCycleId())
                .orElseThrow(() -> new ResourceNotFoundException("ReviewCycle", "id", request.getCycleId()));

        // Check if there is an existing review for this employee, cycle, reviewer, and type
        PerformanceReview review = performanceReviewRepository
                .findByEmployeeIdAndCycleIdAndReviewTypeAndReviewerId(
                        request.getEmployeeId(),
                        request.getCycleId(),
                        request.getReviewType(),
                        request.getReviewerId())
                .orElse(null);

        if (review == null) {
            review = new PerformanceReview();
            review.setEmployee(employee);
            review.setReviewer(reviewer);
            review.setCycle(cycle);
            review.setReviewType(request.getReviewType());
        }

        if (request.getGoalScore() != null) review.setGoalScore(request.getGoalScore());
        if (request.getCompetencyScore() != null) review.setCompetencyScore(request.getCompetencyScore());
        if (request.getPeerScore() != null) review.setPeerScore(request.getPeerScore());
        if (request.getManagerScore() != null) review.setManagerScore(request.getManagerScore());

        // Calculate final score: 60% goal score + 40% competency score
        BigDecimal goalWeight = BigDecimal.valueOf(0.6);
        BigDecimal compWeight = BigDecimal.valueOf(0.4);

        BigDecimal gScore = review.getGoalScore() != null ? review.getGoalScore() : BigDecimal.ZERO;
        BigDecimal cScore = review.getCompetencyScore() != null ? review.getCompetencyScore() : BigDecimal.ZERO;

        BigDecimal finalScore = gScore.multiply(goalWeight).add(cScore.multiply(compWeight)).setScale(2, RoundingMode.HALF_UP);
        review.setFinalScore(finalScore);

        // Map final score to a default band before normalization
        if (request.getBand() != null) {
            review.setBand(request.getBand());
        } else {
            double scoreVal = finalScore.doubleValue();
            if (scoreVal >= 4.5) review.setBand("A+");
            else if (scoreVal >= 4.0) review.setBand("A");
            else if (scoreVal >= 3.5) review.setBand("B+");
            else if (scoreVal >= 3.0) review.setBand("B");
            else if (scoreVal >= 2.0) review.setBand("C");
            else review.setBand("D");
        }

        if (request.getStrengths() != null) review.setStrengths(request.getStrengths());
        if (request.getImprovementAreas() != null) review.setImprovementAreas(request.getImprovementAreas());
        if (request.getManagerComments() != null) review.setManagerComments(request.getManagerComments());
        if (request.getEmployeeComments() != null) review.setEmployeeComments(request.getEmployeeComments());
        
        review.setStatus(request.getStatus() != null ? request.getStatus() : ReviewStatus.DRAFT);
        if (review.getStatus() == ReviewStatus.SUBMITTED) {
            review.setSubmittedAt(LocalDateTime.now());
        }

        return mapToReviewResponse(performanceReviewRepository.save(review));
    }

    @Transactional
    public ReviewResponse acknowledgeReview(Long id, String comments) {
        PerformanceReview review = performanceReviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PerformanceReview", "id", id));
        review.setEmployeeComments(comments);
        review.setStatus(ReviewStatus.ACKNOWLEDGED);
        review.setAcknowledgedAt(LocalDateTime.now());
        return mapToReviewResponse(performanceReviewRepository.save(review));
    }

    // Peer Feedback operations
    @Transactional
    public FeedbackResponse submitPeerFeedback(FeedbackRequest request) {
        Employee fromEmployee = employeeRepository.findById(request.getFromEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", request.getFromEmployeeId()));
        Employee toEmployee = employeeRepository.findById(request.getToEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", request.getToEmployeeId()));
        ReviewCycle cycle = reviewCycleRepository.findById(request.getReviewCycleId())
                .orElseThrow(() -> new ResourceNotFoundException("ReviewCycle", "id", request.getReviewCycleId()));

        PeerFeedback feedback = PeerFeedback.builder()
                .reviewCycle(cycle)
                .fromEmployee(fromEmployee)
                .toEmployee(toEmployee)
                .rating(request.getRating())
                .strengths(request.getStrengths())
                .improvementAreas(request.getImprovementAreas())
                .comments(request.getComments())
                .anonymous(request.isAnonymous())
                .build();

        return mapToFeedbackResponse(peerFeedbackRepository.save(feedback));
    }

    public List<FeedbackResponse> getFeedbackForEmployee(Long cycleId, Long employeeId) {
        return peerFeedbackRepository.findByReviewCycleIdAndToEmployeeId(cycleId, employeeId).stream()
                .map(this::mapToFeedbackResponse)
                .collect(Collectors.toList());
    }

    public List<FeedbackResponse> getFeedbackFromEmployee(Long cycleId, Long employeeId) {
        return peerFeedbackRepository.findByReviewCycleIdAndFromEmployeeId(cycleId, employeeId).stream()
                .map(this::mapToFeedbackResponse)
                .collect(Collectors.toList());
    }

    // Normalization logic: preview & apply
    @Transactional
    public List<NormalizationPreviewResponse> normalizeRatings(Long cycleId, boolean apply) {
        List<PerformanceReview> reviews = performanceReviewRepository.findByCycleId(cycleId).stream()
                .filter(r -> r.getStatus() != ReviewStatus.DRAFT) // only normalize submitted or acknowledged reviews
                .sorted(Comparator.comparing(PerformanceReview::getFinalScore, Comparator.nullsFirst(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        int total = reviews.size();
        List<NormalizationPreviewResponse> previews = new ArrayList<>();

        if (total == 0) {
            return previews;
        }

        // Bell curve target distribution:
        // Top 10% -> A+
        // Next 20% -> A
        // Next 50% -> B
        // Next 15% -> C
        // Bottom 5% -> D
        int aPlusCount = (int) Math.round(total * 0.10);
        int aCount = (int) Math.round(total * 0.20);
        int bCount = (int) Math.round(total * 0.50);
        int cCount = (int) Math.round(total * 0.15);

        for (int i = 0; i < total; i++) {
            PerformanceReview review = reviews.get(i);
            String suggestedBand;
            if (i < aPlusCount) {
                suggestedBand = "A+";
            } else if (i < aPlusCount + aCount) {
                suggestedBand = "A";
            } else if (i < aPlusCount + aCount + bCount) {
                suggestedBand = "B";
            } else if (i < aPlusCount + aCount + bCount + cCount) {
                suggestedBand = "C";
            } else {
                suggestedBand = "D";
            }

            NormalizationPreviewResponse preview = NormalizationPreviewResponse.builder()
                    .reviewId(review.getId())
                    .employeeId(review.getEmployee().getId())
                    .employeeName(review.getEmployee().getFullName())
                    .departmentName(review.getEmployee().getDepartment() != null ? review.getEmployee().getDepartment().getName() : "N/A")
                    .finalScore(review.getFinalScore())
                    .currentBand(review.getBand())
                    .suggestedBand(suggestedBand)
                    .build();

            previews.add(preview);

            if (apply) {
                review.setBand(suggestedBand);
                performanceReviewRepository.save(review);
            }
        }

        return previews;
    }

    private ReviewCycleResponse mapToCycleResponse(ReviewCycle cycle) {
        return ReviewCycleResponse.builder()
                .id(cycle.getId())
                .name(cycle.getName())
                .type(cycle.getType())
                .startDate(cycle.getStartDate())
                .endDate(cycle.getEndDate())
                .status(cycle.getStatus())
                .description(cycle.getDescription())
                .build();
    }

    private ReviewResponse mapToReviewResponse(PerformanceReview review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .employeeId(review.getEmployee().getId())
                .employeeName(review.getEmployee().getFullName())
                .employeeDesignation(review.getEmployee().getDesignation())
                .employeeDepartment(review.getEmployee().getDepartment() != null ? review.getEmployee().getDepartment().getName() : null)
                .reviewerId(review.getReviewer().getId())
                .reviewerName(review.getReviewer().getFullName())
                .cycleId(review.getCycle().getId())
                .cycleName(review.getCycle().getName())
                .reviewType(review.getReviewType())
                .goalScore(review.getGoalScore())
                .competencyScore(review.getCompetencyScore())
                .peerScore(review.getPeerScore())
                .managerScore(review.getManagerScore())
                .finalScore(review.getFinalScore())
                .band(review.getBand())
                .strengths(review.getStrengths())
                .improvementAreas(review.getImprovementAreas())
                .managerComments(review.getManagerComments())
                .employeeComments(review.getEmployeeComments())
                .status(review.getStatus())
                .submittedAt(review.getSubmittedAt())
                .acknowledgedAt(review.getAcknowledgedAt())
                .build();
    }

    private FeedbackResponse mapToFeedbackResponse(PeerFeedback feedback) {
        return FeedbackResponse.builder()
                .id(feedback.getId())
                .reviewCycleId(feedback.getReviewCycle().getId())
                .reviewCycleName(feedback.getReviewCycle().getName())
                .fromEmployeeId(feedback.isAnonymous() ? null : feedback.getFromEmployee().getId())
                .fromEmployeeName(feedback.isAnonymous() ? "Anonymous" : feedback.getFromEmployee().getFullName())
                .toEmployeeId(feedback.getToEmployee().getId())
                .toEmployeeName(feedback.getToEmployee().getFullName())
                .rating(feedback.getRating())
                .strengths(feedback.getStrengths())
                .improvementAreas(feedback.getImprovementAreas())
                .comments(feedback.getComments())
                .anonymous(feedback.isAnonymous())
                .submittedAt(feedback.getSubmittedAt())
                .build();
    }
}
