package com.pothole.dto;

import com.pothole.model.enums.NotificationType;
import java.time.OffsetDateTime;
import java.util.UUID;

public class NotificationResponse {

    private Long id;
    private UUID potholeId;
    private String title;
    private String message;
    private Boolean readStatus;
    private NotificationType notificationType;
    private OffsetDateTime createdAt;

    public NotificationResponse() {
    }

    public NotificationResponse(Long id, UUID potholeId, String title, String message, Boolean readStatus, NotificationType notificationType, OffsetDateTime createdAt) {
        this.id = id;
        this.potholeId = potholeId;
        this.title = title;
        this.message = message;
        this.readStatus = readStatus;
        this.notificationType = notificationType;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getPotholeId() {
        return potholeId;
    }

    public void setPotholeId(UUID potholeId) {
        this.potholeId = potholeId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getReadStatus() {
        return readStatus;
    }

    public void setReadStatus(Boolean readStatus) {
        this.readStatus = readStatus;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(NotificationType notificationType) {
        this.notificationType = notificationType;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
