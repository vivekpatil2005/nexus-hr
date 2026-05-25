package com.nexushr.attendance.controller;

import com.nexushr.attendance.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@RestController
@Tag(name = "Attendance Live Feed", description = "Server-Sent Events for real-time attendance")
public class AttendanceSseController {

    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    @GetMapping(value = "/api/attendance/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'MANAGER')")
    @Operation(summary = "Get Server-Sent Events stream for real-time attendance tracking")
    public SseEmitter streamAttendance() {
        // Emitter with 30 minutes timeout
        SseEmitter emitter = new SseEmitter(1800000L);
        emitters.add(emitter);

        emitter.onCompletion(() -> {
            log.debug("SSE emitter completed. Removing from active emitters.");
            emitters.remove(emitter);
        });

        emitter.onTimeout(() -> {
            log.debug("SSE emitter timed out. Removing from active emitters.");
            emitters.remove(emitter);
        });

        emitter.onError(ex -> {
            log.debug("SSE emitter error: {}. Removing from active emitters.", ex.getMessage());
            emitters.remove(emitter);
        });

        // Send an initial handshake event
        try {
            emitter.send(SseEmitter.event().name("init").data("Connected to live attendance stream."));
        } catch (IOException e) {
            log.error("Failed to send initial SSE handshake", e);
            emitters.remove(emitter);
        }

        return emitter;
    }

    /**
     * Invoked by Redis MessageListenerAdapter when a message is published to the channel.
     */
    public void handleMessage(String message) {
        log.debug("Received live attendance event via Redis: {}", message);
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("attendance-event")
                        .data(message));
            } catch (Exception e) {
                log.debug("Failed to send SSE event, removing emitter.");
                emitters.remove(emitter);
            }
        }
    }

    @Configuration
    public static class RedisSseConfig {

        @Bean
        public RedisMessageListenerContainer sseListenerContainer(
                RedisConnectionFactory connectionFactory,
                MessageListenerAdapter attendanceSseAdapter) {
            RedisMessageListenerContainer container = new RedisMessageListenerContainer();
            container.setConnectionFactory(connectionFactory);
            container.addMessageListener(attendanceSseAdapter, new ChannelTopic(AttendanceService.ATTENDANCE_CHANNEL));
            return container;
        }

        @Bean
        public MessageListenerAdapter attendanceSseAdapter(AttendanceSseController sseController) {
            return new MessageListenerAdapter(sseController, "handleMessage");
        }
    }
}
