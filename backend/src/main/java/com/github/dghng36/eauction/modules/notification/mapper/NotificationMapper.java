package com.github.dghng36.eauction.modules.notification.mapper;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.github.dghng36.eauction.core.utils.MetadataUtils;
import com.github.dghng36.eauction.modules.notification.dto.response.NotificationResponse;
import com.github.dghng36.eauction.modules.notification.enums.NotificationType;
import com.github.dghng36.eauction.modules.notification.model.Notification;

@Component
public class NotificationMapper {
    public Notification toNotificationEntity(
        String receiverId,
        NotificationType type,
        String title,
        String content,
        String referenceId,
        Map<String, Object> metadata
    ) {
        return Notification.builder()
            .receiverId(receiverId)
            .type(type)
            .title(title)
            .content(content)
            .isRead(false)
            .readAt(null)
            .referenceId(referenceId)
            .metadata(MetadataUtils.sanitizeDynamicMetadata(metadata))
            .isDeleted(false)
            .deletedAt(null)
            .build();
    }

    public NotificationResponse toNotificationResponse(Notification notification) {
        if (notification == null) {
            return null;
        }

        return NotificationResponse.builder()
            .id(notification.getId())
            .type(notification.getType().name())
            .content(notification.getContent())
            .isRead(notification.getIsRead())
            .readAt(notification.getReadAt())
            .createdAt(notification.getCreatedAt())
            .referenceId(notification.getReferenceId())
            .build();
    }

    public List<NotificationResponse> toNotificationResponseList(List<Notification> notifications) {
        if (notifications == null) {
            return List.of();
        }
        
        return notifications.stream()
            .map(this::toNotificationResponse)
            .toList();
    }
}
