package com.nexushr.notification.service;

import com.nexushr.notification.entity.Notification;
import com.nexushr.notification.repository.NotificationRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;

    public List<Notification> getNotificationsForUser(Long recipientId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId);
    }

    public List<Notification> getUnreadNotifications(Long recipientId) {
        return notificationRepository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc(recipientId);
    }

    public long getUnreadCount(Long recipientId) {
        return notificationRepository.countByRecipientIdAndReadFalse(recipientId);
    }

    @Transactional
    public Notification createInAppNotification(Long recipientId, String subject, String body) {
        Notification notification = Notification.builder()
                .recipientId(recipientId)
                .type("IN_APP")
                .channel("WEB")
                .subject(subject)
                .body(body)
                .status("DELIVERED")
                .read(false)
                .build();
        return notificationRepository.save(notification);
    }

    @Transactional
    public List<Notification> broadcastInApp(String subject, String body, List<Long> recipientIds) {
        List<Notification> notifications = new ArrayList<>();
        for (Long recipientId : recipientIds) {
            Notification n = Notification.builder()
                    .recipientId(recipientId)
                    .type("ANNOUNCEMENT")
                    .channel("WEB")
                    .subject(subject)
                    .body(body)
                    .status("DELIVERED")
                    .read(false)
                    .build();
            notifications.add(n);
        }
        return notificationRepository.saveAll(notifications);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            n.setReadAt(LocalDateTime.now());
            notificationRepository.save(n);
        });
    }

    @Transactional
    public void markAllAsRead(Long recipientId) {
        List<Notification> unread = notificationRepository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc(recipientId);
        LocalDateTime now = LocalDateTime.now();
        unread.forEach(n -> {
            n.setRead(true);
            n.setReadAt(now);
        });
        notificationRepository.saveAll(unread);
    }

    @Transactional
    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
    }

    @Async
    @Transactional
    public void sendHtmlEmail(Long recipientId, String toEmail, String subject, String htmlContent) {
        log.info("Preparing to send email asynchronously to {} (recipientId: {})", toEmail, recipientId);

        Notification notification = Notification.builder()
                .recipientId(recipientId)
                .type("EMAIL")
                .channel("SMTP")
                .subject(subject)
                .body(htmlContent)
                .status("PENDING")
                .read(false)
                .build();

        notification = notificationRepository.save(notification);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            helper.setText(htmlContent, true);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setFrom("no-reply@nexushr.com");

            mailSender.send(mimeMessage);

            notification.setStatus("SENT");
            notification.setSentAt(LocalDateTime.now());
            notificationRepository.save(notification);
            log.info("Email sent successfully to {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send email to {}", toEmail, e);
            notification.setStatus("FAILED");
            notification.setErrorMessage(e.getMessage());
            notificationRepository.save(notification);
        }
    }
}
