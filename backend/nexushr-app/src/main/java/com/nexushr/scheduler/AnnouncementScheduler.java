package com.nexushr.scheduler;

import com.nexushr.auth.repository.UserRepository;
import com.nexushr.notification.service.NotificationService;
import com.nexushr.performance.entity.Announcement;
import com.nexushr.performance.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnnouncementScheduler {

    private final AnnouncementService announcementService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @Scheduled(fixedRate = 60000)
    public void checkScheduledAnnouncements() {
        log.debug("Checking for scheduled announcements to publish...");
        List<Announcement> published = announcementService.publishScheduledAnnouncements();
        
        if (!published.isEmpty()) {
            log.info("Published {} scheduled announcements automatically", published.size());
            
            List<Long> allUserIds = userRepository.findAll().stream()
                    .filter(u -> u.isEnabled())
                    .map(u -> u.getId())
                    .collect(Collectors.toList());
            
            for (Announcement a : published) {
                String subject = "📢 " + a.getTitle();
                String body = a.getContent().substring(0, Math.min(200, a.getContent().length()));
                if (a.getContent().length() > 200) {
                    body += "...";
                }
                
                try {
                    notificationService.broadcastInApp(subject, body, allUserIds);
                    log.info("Broadcasted notification for auto-published announcement: '{}' to {} users", a.getTitle(), allUserIds.size());
                } catch (Exception e) {
                    log.error("Failed to broadcast notification for announcement ID {}: {}", a.getId(), e.getMessage(), e);
                }
            }
        }
    }
}
