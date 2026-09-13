package com.pothole.controller;

import com.pothole.dto.NotificationResponse;
import com.pothole.model.User;
import com.pothole.service.AuthService;
import com.pothole.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final AuthService authService;

    public NotificationController(NotificationService notificationService, AuthService authService) {
        this.notificationService = notificationService;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getUserNotifications(
            Authentication authentication,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        User user = authService.getUserByEmail(authentication.getName());
        Page<NotificationResponse> notifications = notificationService.getUserNotifications(user, PageRequest.of(page, size));
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread")
    public ResponseEntity<List<NotificationResponse>> getUnreadNotifications(Authentication authentication) {
        User user = authService.getUserByEmail(authentication.getName());
        List<NotificationResponse> unread = notificationService.getUnreadNotifications(user);
        return ResponseEntity.ok(unread);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication authentication) {
        User user = authService.getUserByEmail(authentication.getName());
        long count = notificationService.getUnreadCount(user);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            Authentication authentication,
            @PathVariable("id") Long notificationId
    ) {
        User user = authService.getUserByEmail(authentication.getName());
        notificationService.markAsRead(user, notificationId);
        return ResponseEntity.noContent().build();
    }
}
