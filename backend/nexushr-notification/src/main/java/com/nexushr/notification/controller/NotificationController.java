package com.nexushr.notification.controller;

import com.nexushr.common.dto.ApiResponse;
import com.nexushr.common.security.SecurityUtils;
import com.nexushr.notification.entity.Notification;
import com.nexushr.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "User notifications inbox and management")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PersistenceContext
    private EntityManager entityManager;

    @GetMapping("/me")
    @Operation(summary = "Get all notifications for the logged-in user")
    public ResponseEntity<ApiResponse<List<Notification>>> getMyNotifications() {
        Long userId = resolveCurrentUserId();
        if (userId == null) {
            return ResponseEntity.ok(ApiResponse.success("Unauthenticated", Collections.emptyList()));
        }
        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved", notificationService.getNotificationsForUser(userId)));
    }

    @GetMapping("/me/unread")
    @Operation(summary = "Get unread notifications for the logged-in user")
    public ResponseEntity<ApiResponse<List<Notification>>> getMyUnreadNotifications() {
        Long userId = resolveCurrentUserId();
        if (userId == null) {
            return ResponseEntity.ok(ApiResponse.success(Collections.emptyList()));
        }
        return ResponseEntity.ok(ApiResponse.success(notificationService.getUnreadNotifications(userId)));
    }

    @GetMapping("/me/unread-count")
    @Operation(summary = "Get unread notification count")
    public ResponseEntity<ApiResponse<Long>> getMyUnreadCount() {
        Long userId = resolveCurrentUserId();
        if (userId == null) {
            return ResponseEntity.ok(ApiResponse.success(0L));
        }
        return ResponseEntity.ok(ApiResponse.success(notificationService.getUnreadCount(userId)));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", null));
    }

    @PatchMapping("/me/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead() {
        Long userId = resolveCurrentUserId();
        if (userId != null) {
            notificationService.markAllAsRead(userId);
        }
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", null));
    }

    @PostMapping("/broadcast")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'MANAGER')")
    @Operation(summary = "Broadcast an in-app notification to multiple users")
    public ResponseEntity<ApiResponse<List<Notification>>> broadcastNotification(@RequestBody Map<String, Object> body) {
        String subject = (String) body.get("subject");
        String message = (String) body.get("message");
        @SuppressWarnings("unchecked")
        List<Integer> rawIds = (List<Integer>) body.get("recipientIds");
        List<Long> recipientIds = rawIds.stream().map(Integer::longValue).toList();

        List<Notification> notifications = notificationService.broadcastInApp(subject, message, recipientIds);
        log.info("Broadcast notification '{}' to {} users", subject, recipientIds.size());
        return ResponseEntity.ok(ApiResponse.success("Notification broadcast sent", notifications));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'MANAGER')")
    @Operation(summary = "Delete a notification")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.ok(ApiResponse.success("Notification deleted", null));
    }

    private Long resolveCurrentUserId() {
        String email = SecurityUtils.getCurrentUsername().orElse(null);
        if (email == null) return null;
        try {
            return ((Number) entityManager.createNativeQuery(
                    "SELECT id FROM users WHERE email = :email OR username = :email")
                    .setParameter("email", email)
                    .getSingleResult()).longValue();
        } catch (Exception e) {
            log.warn("Could not resolve userId for {}: {}", email, e.getMessage());
            return null;
        }
    }
}
