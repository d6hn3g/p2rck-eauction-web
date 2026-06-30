package com.github.dghng36.eauction.modules.notification.dto.response;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class NotificationResponse {
    String id;

    String type;

    String content;

    Boolean isRead;
    Instant readAt;

    Instant createdAt;

    String referenceId;
}
