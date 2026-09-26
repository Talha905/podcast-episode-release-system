package com.podcastrelease.controller;

import com.podcastrelease.model.Notification;
import com.podcastrelease.model.User;
import com.podcastrelease.repository.NotificationRepository;
import com.podcastrelease.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationController(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getNotifications(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.ok(Map.of("unreadCount", 0, "notifications", List.of()));
        }

        User currentUser = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (currentUser == null) {
            return ResponseEntity.ok(Map.of("unreadCount", 0, "notifications", List.of()));
        }

        long unreadCount = notificationRepository.countByRecipientIdAndIsReadFalse(currentUser.getId());
        List<Notification> list = notificationRepository.findTop10ByRecipientIdOrderByCreatedAtDesc(currentUser.getId());

        List<Map<String, Object>> notifications = list.stream().map(n -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", n.getId());
            map.put("title", n.getTitle());
            map.put("message", n.getMessage());
            map.put("type", n.getType());
            map.put("targetUrl", n.getTargetUrl());
            map.put("isRead", n.isRead());
            map.put("createdAt", n.getCreatedAt().toString());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "unreadCount", unreadCount,
                "notifications", notifications
        ));
    }

    @PostMapping("/{id}/read")
    @Transactional
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable Long id, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        User currentUser = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (currentUser == null) return ResponseEntity.status(401).build();

        Notification n = notificationRepository.findById(id).orElse(null);
        if (n != null && n.getRecipient().getId().equals(currentUser.getId())) {
            n.setRead(true);
            notificationRepository.save(n);
        }

        return ResponseEntity.ok(Map.of("success", true));
    }
}
