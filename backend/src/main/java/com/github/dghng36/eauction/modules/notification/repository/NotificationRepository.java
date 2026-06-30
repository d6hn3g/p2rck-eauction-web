package com.github.dghng36.eauction.modules.notification.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.github.dghng36.eauction.modules.notification.model.Notification;

public interface NotificationRepository extends MongoRepository<Notification, String> {
    Optional<Notification> findByIdAndReceiverIdAndIsDeletedFalse(String id, String receiverId);

    Optional<Notification> findByIdAndIsDeletedFalse(String id);
    Page<Notification> findAllByReceiverIdAndIsDeletedFalse(String receiverId, Pageable pageable);

    long countByReceiverIdAndIsDeletedFalseAndReadAtIsNull(String receiverId);
}
