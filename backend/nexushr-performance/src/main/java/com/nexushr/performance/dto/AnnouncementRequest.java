package com.nexushr.performance.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AnnouncementRequest {
    private String title;
    private String content;
    private Long authorId;
    private Long departmentId;
    private String priority;
    private boolean published;
    private LocalDateTime publishedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime scheduledAt;
}
