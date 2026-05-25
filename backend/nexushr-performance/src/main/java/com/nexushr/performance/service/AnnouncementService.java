package com.nexushr.performance.service;

import com.nexushr.common.exception.ResourceNotFoundException;
import com.nexushr.employee.entity.Department;
import com.nexushr.employee.entity.Employee;
import com.nexushr.employee.repository.DepartmentRepository;
import com.nexushr.employee.repository.EmployeeRepository;
import com.nexushr.performance.dto.AnnouncementRequest;
import com.nexushr.performance.dto.AnnouncementResponse;
import com.nexushr.performance.entity.Announcement;
import com.nexushr.performance.repository.AnnouncementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;

    public List<AnnouncementResponse> getPublishedAnnouncements() {
        return announcementRepository.findByPublishedTrueOrderByPublishedAtDesc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<AnnouncementResponse> getPublishedAnnouncementsByDepartment(Long departmentId) {
        if (departmentId == null) {
            return announcementRepository.findByPublishedTrueAndDepartmentIdIsNullOrderByPublishedAtDesc().stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }
        return announcementRepository.findByPublishedTrueAndDepartmentIdOrderByPublishedAtDesc(departmentId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<AnnouncementResponse> getAllAnnouncements() {
        return announcementRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AnnouncementResponse createAnnouncement(AnnouncementRequest request) {
        Department department = null;
        if (request.getDepartmentId() != null) {
            department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.getDepartmentId()));
        }

        LocalDateTime scheduledAt = request.getScheduledAt();
        boolean published = request.isPublished();
        LocalDateTime publishedAt = null;

        if (scheduledAt != null && scheduledAt.isAfter(LocalDateTime.now())) {
            published = false;
        } else {
            if (published) {
                publishedAt = request.getPublishedAt() != null ? request.getPublishedAt() : LocalDateTime.now();
            }
        }

        Announcement announcement = Announcement.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .authorId(request.getAuthorId())
                .department(department)
                .priority(request.getPriority() != null ? request.getPriority() : "NORMAL")
                .published(published)
                .publishedAt(publishedAt)
                .expiresAt(request.getExpiresAt())
                .scheduledAt(scheduledAt)
                .build();

        return mapToResponse(announcementRepository.save(announcement));
    }

    @Transactional
    public AnnouncementResponse updateAnnouncement(Long id, AnnouncementRequest request) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", id));

        if (request.getTitle() != null) announcement.setTitle(request.getTitle());
        if (request.getContent() != null) announcement.setContent(request.getContent());
        if (request.getPriority() != null) announcement.setPriority(request.getPriority());
        if (request.getExpiresAt() != null) announcement.setExpiresAt(request.getExpiresAt());
        
        if (request.getScheduledAt() != null) {
            announcement.setScheduledAt(request.getScheduledAt());
            if (request.getScheduledAt().isAfter(LocalDateTime.now())) {
                announcement.setPublished(false);
                announcement.setPublishedAt(null);
            }
        } else {
            announcement.setScheduledAt(null);
        }

        if (request.isPublished() && !announcement.isPublished()) {
            announcement.setPublished(true);
            announcement.setPublishedAt(request.getPublishedAt() != null ? request.getPublishedAt() : LocalDateTime.now());
        } else if (!request.isPublished() && announcement.isPublished()) {
            announcement.setPublished(false);
            announcement.setPublishedAt(null);
        }

        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.getDepartmentId()));
            announcement.setDepartment(department);
        }

        return mapToResponse(announcementRepository.save(announcement));
    }

    @Transactional
    public void deleteAnnouncement(Long id) {
        if (!announcementRepository.existsById(id)) {
            throw new ResourceNotFoundException("Announcement", "id", id);
        }
        announcementRepository.deleteById(id);
    }

    @Transactional
    public List<Announcement> publishScheduledAnnouncements() {
        LocalDateTime now = LocalDateTime.now();
        List<Announcement> pending = announcementRepository.findByPublishedFalseAndScheduledAtIsNotNullAndScheduledAtBefore(now);
        for (Announcement a : pending) {
            a.setPublished(true);
            a.setPublishedAt(now);
        }
        if (!pending.isEmpty()) {
            return announcementRepository.saveAll(pending);
        }
        return pending;
    }

    private AnnouncementResponse mapToResponse(Announcement announcement) {
        String authorName = "HR Administrator";
        if (announcement.getAuthorId() != null) {
            authorName = employeeRepository.findById(announcement.getAuthorId())
                    .map(Employee::getFullName)
                    .orElse("HR Administrator");
        }

        return AnnouncementResponse.builder()
                .id(announcement.getId())
                .title(announcement.getTitle())
                .content(announcement.getContent())
                .authorId(announcement.getAuthorId())
                .authorName(authorName)
                .departmentId(announcement.getDepartment() != null ? announcement.getDepartment().getId() : null)
                .departmentName(announcement.getDepartment() != null ? announcement.getDepartment().getName() : "All Departments")
                .priority(announcement.getPriority())
                .published(announcement.isPublished())
                .publishedAt(announcement.getPublishedAt())
                .expiresAt(announcement.getExpiresAt())
                .scheduledAt(announcement.getScheduledAt())
                .createdAt(announcement.getCreatedAt())
                .updatedAt(announcement.getUpdatedAt())
                .build();
    }
}
