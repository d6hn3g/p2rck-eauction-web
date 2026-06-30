package com.github.dghng36.eauction.modules.social.conversation.dto.internal;

import com.github.dghng36.eauction.modules.social.enums.ConversationType;

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
public class ConversationInfo {
    String id;

    String title;

    ConversationType type;
}
