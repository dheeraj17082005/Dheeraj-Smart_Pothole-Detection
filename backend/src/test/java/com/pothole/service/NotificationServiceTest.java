package com.pothole.service;

import com.pothole.dto.NotificationResponse;
import com.pothole.model.Notification;
import com.pothole.model.Pothole;
import com.pothole.model.User;
import com.pothole.model.enums.NotificationType;
import com.pothole.model.enums.Role;
import com.pothole.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private User sampleUser;
    private Notification sampleNotification;

    @BeforeEach
    void setUp() {
        sampleUser = new User("user@example.com", "pass", "Aman Kumar", null, Role.ROLE_USER);
        sampleUser.setId(100L);

        sampleNotification = new Notification(sampleUser, null, "Report Status Updated", "Your report status is now IN_PROGRESS", NotificationType.STATUS_CHANGE);
        sampleNotification.setId(500L);
    }

    @Test
    @DisplayName("Should create and save a new notification")
    void createNotification_Success() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(sampleNotification);

        Notification created = notificationService.createNotification(sampleUser, null, "Report Status Updated", "Your report status is now IN_PROGRESS", NotificationType.STATUS_CHANGE);

        assertNotNull(created);
        assertEquals("Report Status Updated", created.getTitle());
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("Should return unread notifications for user")
    void getUnreadNotifications_Success() {
        when(notificationRepository.findByUserAndReadStatusFalseOrderByCreatedAtDesc(sampleUser))
                .thenReturn(List.of(sampleNotification));

        List<NotificationResponse> list = notificationService.getUnreadNotifications(sampleUser);

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("Report Status Updated", list.get(0).getTitle());
    }

    @Test
    @DisplayName("Should mark notification as read when owned by user")
    void markAsRead_Owner_Success() {
        when(notificationRepository.findById(500L)).thenReturn(Optional.of(sampleNotification));

        notificationService.markAsRead(sampleUser, 500L);

        assertTrue(sampleNotification.getReadStatus());
        verify(notificationRepository).save(sampleNotification);
    }

    @Test
    @DisplayName("Should throw 403 Forbidden when marking notification owned by another user")
    void markAsRead_NonOwner_Throws403() {
        User otherUser = new User("other@example.com", "pass", "Other User", null, Role.ROLE_USER);
        otherUser.setId(999L);

        when(notificationRepository.findById(500L)).thenReturn(Optional.of(sampleNotification));

        assertThrows(ResponseStatusException.class, () -> notificationService.markAsRead(otherUser, 500L));
        verify(notificationRepository, never()).save(any(Notification.class));
    }
}
