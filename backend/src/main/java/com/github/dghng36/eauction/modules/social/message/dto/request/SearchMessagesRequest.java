package com.github.dghng36.eauction.modules.social.message.dto.request;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class SearchMessagesRequest {
    String keyword;

    String senderId;

    String messageType;

    Instant fromDate;
    Instant toDate;

    Boolean hasAttachment;
}
