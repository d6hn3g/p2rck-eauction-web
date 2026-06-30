package com.github.dghng36.eauction.modules.notification.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.modules.notification.dto.response.NotificationSocketResponse;
import com.github.dghng36.eauction.modules.notification.model.Notification;
import com.github.dghng36.eauction.modules.notification.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class InternalNotificationService {
    NotificationRepository notificationRepo;
    
    public NotificationSocketResponse getNotificationById(String notificationId) {
        Notification notification = notificationRepo.findByIdAndIsDeletedFalse(notificationId)
            .orElseThrow(() -> new AppException("Notification not found", HttpStatus.NOT_FOUND));    
        
        return NotificationSocketResponse.builder()
            .id(notification.getId())
            .type(notification.getType().name())
            .title(notification.getTitle())
            .content(notification.getContent())
            .referenceId(notification.getReferenceId())
            .createdAt(notification.getCreatedAt())
            .build();
    }
}
