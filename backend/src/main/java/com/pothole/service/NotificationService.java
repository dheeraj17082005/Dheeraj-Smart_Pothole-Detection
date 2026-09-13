package com.pothole.service;

import com.pothole.dto.NotificationResponse;
import com.pothole.model.Notification;
import com.pothole.model.Pothole;
import com.pothole.model.User;
import com.pothole.model.enums.NotificationType;
import com.pothole.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public Notification createNotification(User user, Pothole pothole, String title, String message, NotificationType type) {
        Notification notification = new Notification(user, pothole, title, message, type);
        return notificationRepository.save(notification);
    }

    public Page<NotificationResponse> getUserNotifications(User user, Pageable pageable) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                .map(this::mapToResponse);
    }

    public List<NotificationResponse> getUnreadNotifications(User user) {
        return notificationRepository.findByUserAndReadStatusFalseOrderByCreatedAtDesc(user).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public long getUnreadCount(User user) {
        return notificationRepository.countByUserAndReadStatusFalse(user);
    }

    @Transactional
    public void markAsRead(User user, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to notification");
        }

        notification.setReadStatus(true);
        notificationRepository.save(notification);
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getPothole() != null ? notification.getPothole().getId() : null,
                notification.getTitle(),
                notification.getMessage(),
                notification.getReadStatus(),
                notification.getNotificationType(),
                notification.getCreatedAt()
        );
    }
}
