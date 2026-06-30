package com.github.dghng36.eauction.modules.notification.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.github.dghng36.eauction.core.base.PageResponse;
import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.core.utils.SortUtils;
import com.github.dghng36.eauction.modules.notification.dto.request.SearchNotificationsRequest;
import com.github.dghng36.eauction.modules.notification.dto.response.NotificationCountResponse;
import com.github.dghng36.eauction.modules.notification.dto.response.NotificationResponse;
import com.github.dghng36.eauction.modules.notification.enums.NotificationType;
import com.github.dghng36.eauction.modules.notification.event.NotificationCreatedEvent;
import com.github.dghng36.eauction.modules.notification.event.NotificationReadEvent;
import com.github.dghng36.eauction.modules.notification.mapper.NotificationMapper;
import com.github.dghng36.eauction.modules.notification.model.Notification;
import com.github.dghng36.eauction.modules.notification.repository.NotificationRepository;
import com.mongodb.client.result.UpdateResult;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class NotificationService {
    MongoTemplate mongoTemplate;
    NotificationRepository notificationRepo;

    NotificationMapper notificationMapper;

    ApplicationEventPublisher eventPublisher;

    static Set<String> ALLOWED_SORT_BY_FIELD = Set.of(
        "createdAt",
        "updatedAt",
        "readAt",
        "title"
    );

    public PageResponse<NotificationResponse> getNotifications(
        String userId,
        int page, int size,
        String sortBy, String sortDirection
    ) {
        // Build sort object
        Sort sortBuilt = SortUtils.buildSort(sortBy, sortDirection, ALLOWED_SORT_BY_FIELD);

        // Get all notifications of  user
        Page<Notification> notificationsPage = notificationRepo.findAllByReceiverIdAndIsDeletedFalse(
            userId,
            PageRequest.of(page, size, sortBuilt)
        );

        // Map to response
        List<NotificationResponse> notificationResponses = notificationMapper.toNotificationResponseList(
            notificationsPage.getContent()
        );
        
        return PageResponse.<NotificationResponse>builder()
            .currentPage(notificationsPage.getTotalElements() == 0 ? 0 : page + 1)
            .pageSize(size)
            .totalPages(notificationsPage.getTotalPages())
            .totalElements(notificationsPage.getTotalElements())
            .data(notificationResponses)
            .build();
    }

    public PageResponse<NotificationResponse> searchNotifications(
        String userId, 
        SearchNotificationsRequest searchNotificationsRequest, 
        int page, int size,
        String sortBy, String sortDirection
    ) {
        // Create new query and criteria List
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        criteriaList.add(Criteria.where("isDeleted").is(false));

        // Check if keyword exists
        if (StringUtils.hasText(searchNotificationsRequest.getKeyword())) {
            String regex = ".*" + Pattern.quote(searchNotificationsRequest.getKeyword()) + ".*";

            criteriaList.add(new Criteria().orOperator(
                Criteria.where("title").regex(regex, "i"),
                Criteria.where("content").regex(regex, "i")
            ));
        }

        if (searchNotificationsRequest.getIsRead() != null) {
            criteriaList.add(Criteria.where("isRead").is(searchNotificationsRequest.getIsRead()));
        }

        if (StringUtils.hasText(searchNotificationsRequest.getType())) {
            NotificationType notType = NotificationType.fromString(searchNotificationsRequest.getType())
                .orElseThrow(() -> new AppException("Invalid notification type", HttpStatus.BAD_REQUEST));

            criteriaList.add(Criteria.where("type").is(notType));
        }

        // Build query
        if (!criteriaList.isEmpty()) {
            criteriaList.forEach(query::addCriteria);
        }

        // Validate sort by and sort direction
        Sort sortBuilt = SortUtils.buildSort(sortBy, sortDirection, ALLOWED_SORT_BY_FIELD);

        // Count total elements
        long totalElements = mongoTemplate.count(query, Notification.class);

        // Add pagination and sorting
        // Query pageableQuery = Query.of(query).with(PageRequest.of(page, size, sortBuilt));

        query.with(PageRequest.of(page, size, sortBuilt));

        // Execute query
        // List<Notification> notifications = mongoTemplate.find(pageableQuery, Notification.class);
        List<Notification> notifications = mongoTemplate.find(query, Notification.class);

        // Map to response
        List<NotificationResponse> notificationResponses = notificationMapper.toNotificationResponseList(notifications);

        return PageResponse.<NotificationResponse>builder()
            .currentPage(totalElements == 0 ? 0 : page + 1)
            .pageSize(size)
            .totalPages((int) Math.ceil((double) totalElements / size))
            .totalElements(totalElements)
            .data(notificationResponses)
            .build();
    }

    public NotificationResponse getNotificationDetail(
        String userId, String notificationId
    ) {
        // Get notification of user by id
        Notification notification = notificationRepo.findByIdAndReceiverIdAndIsDeletedFalse(notificationId, userId)
            .orElseThrow(() -> new AppException("Notification not found", HttpStatus.NOT_FOUND));

        // Map to response
        return notificationMapper.toNotificationResponse(notification);
    }

    public NotificationCountResponse getUnreadNotificationCount(String userId) {
        long unReadCount = notificationRepo.countByReceiverIdAndIsDeletedFalseAndReadAtIsNull(userId);

        return NotificationCountResponse.builder()
            .unreadCount(unReadCount)
            .build();
    }

    public void markNotificationAsRead(String userId, String notificationId) {
        Query query = new Query(
            Criteria.where("id").is(notificationId)
                    .and("receiverId").is(userId)
                    .and("isRead").ne(true)
                    .and("isDeleted").is(false)
        );

        Update update = new Update()
            .set("isRead", true)
            .set("readAt", Instant.now());

        UpdateResult result = mongoTemplate.updateFirst(query, update, Notification.class);

        if (result.getMatchedCount() == 0) {
            boolean alreadyRead = mongoTemplate.exists(
                new Query(Criteria.where("id").is(notificationId).and("receiverId").is(userId).and("isDeleted").is(false)),
                Notification.class
            );
            
            if (alreadyRead) {
                return;
            }
            
            throw new AppException("Notification not found or unauthorized", HttpStatus.NOT_FOUND);
        }

        eventPublisher.publishEvent(
            NotificationReadEvent.builder()
                .notificationId(notificationId)
                .receiverId(userId)
                .build()  
        );

        log.info("Notification: [{}] successfully marked as read for user: [{}]", notificationId, userId);
    }

    public void markAllNotificationsAsRead(String userId) {
        Query query = new Query();

        query.addCriteria(
            Criteria.where("receiverId").is(userId)
                .and("isRead").is(false)
                .and("isDeleted").is(false)
        );

        Update update = new Update();
        update.set("isRead", true);
        update.set("readAt", Instant.now());

        mongoTemplate.updateMulti(query, update, Notification.class);

    }

    public void deleteNotification(String userId, String notificationId) {
        Query query = new Query(
            Criteria.where("id").is(notificationId)
                    .and("receiverId").is(userId)
                    .and("isDeleted").is(false)
        );

        Instant now = Instant.now();
        Update update = new Update()
            .set("isRead", true)
            .set("readAt", now)
            .set("isDeleted", true)
            .set("deletedAt", LocalDateTime.now());

        UpdateResult result = mongoTemplate.updateFirst(query, update, Notification.class);

        if (result.getMatchedCount() == 0) {
            log.warn("User: [{}] critical tried to delete non-existent, already deleted, or unauthorized notification: [{}]", userId, notificationId);
            throw new AppException("Notification not found", HttpStatus.NOT_FOUND);
        }

        log.info("Notification: [{}] for user: [{}] has been successfully soft-deleted", notificationId, userId);
    }

    @Transactional
    public Notification createNotification(
        String receiverId,
        NotificationType type,
        String title,
        String content,
        String referenceId,
        Map<String, Object> metadata
    ) {
        Notification notification = notificationMapper.toNotificationEntity(
            receiverId, type, title, content, referenceId, metadata
        );

        Notification savedNotification = notificationRepo.save(notification);

        eventPublisher.publishEvent(
            NotificationCreatedEvent.builder()
                .notificationId(savedNotification.getId())
                .receiverId(receiverId)
                .build()  
        );

        return savedNotification;
    }
}
