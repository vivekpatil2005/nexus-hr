package com.nexushr.performance.repository;

import com.nexushr.performance.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    List<Announcement> findByPublishedTrueOrderByPublishedAtDesc();
    List<Announcement> findByPublishedTrueAndDepartmentIdIsNullOrderByPublishedAtDesc();
    List<Announcement> findByPublishedTrueAndDepartmentIdOrderByPublishedAtDesc(Long departmentId);
    List<Announcement> findByPublishedFalseAndScheduledAtIsNotNullAndScheduledAtBefore(LocalDateTime now);
}
