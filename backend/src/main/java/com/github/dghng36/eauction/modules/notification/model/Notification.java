package com.github.dghng36.eauction.modules.notification.model;

import java.time.Instant;
import java.util.Map;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.github.dghng36.eauction.core.base.BaseEntity;
import com.github.dghng36.eauction.modules.notification.enums.NotificationType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Document(collection = "notifications")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class Notification extends BaseEntity {
    @Indexed
    String receiverId;

    NotificationType type;

    String title;

    String content;

    Boolean isRead;
    Instant readAt;

    String referenceId;

    Map<String, Object> metadata;
}
