package com.github.dghng36.eauction.modules.social.message.event;

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
public class MessageReadEvent {
    String lastReadMessageId;
    
    String conversationId;

    String readerId;
}
