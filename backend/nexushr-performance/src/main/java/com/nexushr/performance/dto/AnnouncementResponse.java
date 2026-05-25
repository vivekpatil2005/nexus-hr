package com.nexushr.performance.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class AnnouncementResponse {
    private Long id;
    private String title;
    private String content;
    private Long authorId;
    private String authorName;
    private Long departmentId;
    private String departmentName;
    private String priority;
    private boolean published;
    private LocalDateTime publishedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime scheduledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
