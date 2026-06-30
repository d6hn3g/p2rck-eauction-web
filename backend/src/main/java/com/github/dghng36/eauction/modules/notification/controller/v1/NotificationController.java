package com.github.dghng36.eauction.modules.notification.controller.v1;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.dghng36.eauction.core.base.ApiResponse;
import com.github.dghng36.eauction.core.base.PageResponse;
import com.github.dghng36.eauction.infra.config.security.annotation.AuthInfo;
import com.github.dghng36.eauction.infra.config.security.annotation.AuthInfoType;
import com.github.dghng36.eauction.modules.notification.dto.request.SearchNotificationsRequest;
import com.github.dghng36.eauction.modules.notification.dto.response.NotificationCountResponse;
import com.github.dghng36.eauction.modules.notification.dto.response.NotificationResponse;
import com.github.dghng36.eauction.modules.notification.service.NotificationService;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
@Slf4j
public class NotificationController {
    NotificationService notificationService;

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @GetMapping
    @Validated
    ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getNotifications(
        @AuthInfo(info = AuthInfoType.ID) String userId,

        @RequestParam(defaultValue = "0") 
        @Min(value = 0, message = "Page index must not be less than 0")
        int page,

        @RequestParam(defaultValue = "10") 
        @Min(value = 1, message = "Page size must be at least 1")
        @Max(value = 50, message = "Page size must not be greater than 50")
        int size,
        
        @RequestParam(defaultValue = "createdAt") String sortBy,
        @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        PageResponse<NotificationResponse> notifications = notificationService.getNotifications(
            userId, 
            page, size, 
            sortBy, sortDirection
        );

        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved successfully", notifications));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @PostMapping("/search")
    @Validated
    ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> searchNotifications(
        @AuthInfo(info = AuthInfoType.ID) String userId,

        @RequestParam(defaultValue = "0") 
        @Min(value = 0, message = "Page index must not be less than 0")
        int page,

        @RequestParam(defaultValue = "10") 
        @Min(value = 1, message = "Page size must be at least 1")
        @Max(value = 50, message = "Page size must not be greater than 50")
        int size,
        
        @RequestParam(defaultValue = "createdAt") String sortBy,
        @RequestParam(defaultValue = "desc") String sortDirection,

        @RequestBody SearchNotificationsRequest searchNotificationsRequest
    ) {
        PageResponse<NotificationResponse> notifications = notificationService.searchNotifications(
            userId, searchNotificationsRequest, 
            page, size, 
            sortBy, sortDirection
        );

        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved successfully", notifications));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @GetMapping("/{notificationId}")
    ResponseEntity<ApiResponse<NotificationResponse>> getNotificationDetail(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @PathVariable String notificationId
    ) {
        NotificationResponse notificationDetail = notificationService.getNotificationDetail(userId, notificationId);

        return ResponseEntity.ok(ApiResponse.success("Notification detail retrieved successfully", notificationDetail));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @GetMapping("/unread")
    ResponseEntity<ApiResponse<NotificationCountResponse>> getUnreadNotificationCount(
        @AuthInfo(info = AuthInfoType.ID) String userId
    ) {
        NotificationCountResponse notificationUnReadResponse = notificationService.getUnreadNotificationCount(userId);

        return ResponseEntity.ok(ApiResponse.success("Unread notification count retrieved successfully", notificationUnReadResponse));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @PutMapping("/{notificationId}/read")
    ResponseEntity<ApiResponse<Void>> markNotificationAsRead(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @PathVariable String notificationId
    ) {
        notificationService.markNotificationAsRead(userId, notificationId);

        return ResponseEntity.ok(ApiResponse.success("Notification marked as read successfully"));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @PutMapping("/read")
    ResponseEntity<ApiResponse<Void>> markAllNotificationsAsRead(
        @AuthInfo(info = AuthInfoType.ID) String userId
    ) {
        notificationService.markAllNotificationsAsRead(userId);

        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read successfully"));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @DeleteMapping("/{notificationId}")
    ResponseEntity<ApiResponse<Void>> deleteNotification(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @PathVariable String notificationId
    ) {
        log.info("Deleting notification with ID: [{}] for user: [{}]", notificationId, userId);

        notificationService.deleteNotification(userId, notificationId);

        return ResponseEntity.ok(ApiResponse.success("Notification deleted successfully"));
    }
}
