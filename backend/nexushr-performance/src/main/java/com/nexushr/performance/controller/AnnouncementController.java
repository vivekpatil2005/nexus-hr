package com.nexushr.performance.controller;

import com.nexushr.common.dto.ApiResponse;
import com.nexushr.performance.dto.AnnouncementRequest;
import com.nexushr.performance.dto.AnnouncementResponse;
import com.nexushr.performance.service.AnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
@Tag(name = "Announcements", description = "Company announcements and news portal")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @GetMapping
    @Operation(summary = "Get all active published announcements")
    public ResponseEntity<ApiResponse<List<AnnouncementResponse>>> getPublishedAnnouncements() {
        return ResponseEntity.ok(ApiResponse.success("Announcements retrieved successfully", announcementService.getPublishedAnnouncements()));
    }

    @GetMapping("/department/{deptId}")
    @Operation(summary = "Get published announcements for a department")
    public ResponseEntity<ApiResponse<List<AnnouncementResponse>>> getAnnouncementsByDepartment(@PathVariable Long deptId) {
        return ResponseEntity.ok(ApiResponse.success("Announcements retrieved successfully", announcementService.getPublishedAnnouncementsByDepartment(deptId)));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'MANAGER')")
    @Operation(summary = "Get all announcements (including unpublished ones for admins/HR)")
    public ResponseEntity<ApiResponse<List<AnnouncementResponse>>> getAllAnnouncements() {
        return ResponseEntity.ok(ApiResponse.success("All announcements retrieved successfully", announcementService.getAllAnnouncements()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'MANAGER')")
    @Operation(summary = "Create an announcement")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> createAnnouncement(@Valid @RequestBody AnnouncementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Announcement created successfully", announcementService.createAnnouncement(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'MANAGER')")
    @Operation(summary = "Update an announcement")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> updateAnnouncement(@PathVariable Long id, @Valid @RequestBody AnnouncementRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Announcement updated successfully", announcementService.updateAnnouncement(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'MANAGER')")
    @Operation(summary = "Delete an announcement")
    public ResponseEntity<ApiResponse<Void>> deleteAnnouncement(@PathVariable Long id) {
        announcementService.deleteAnnouncement(id);
        return ResponseEntity.ok(ApiResponse.success("Announcement deleted successfully", null));
    }
}
