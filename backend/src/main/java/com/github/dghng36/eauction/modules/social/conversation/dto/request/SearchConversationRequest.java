package com.github.dghng36.eauction.modules.social.conversation.dto.request;

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
public class SearchConversationRequest {
    String keyWord;

    Boolean pinned;

    Boolean muted;

    Boolean active;

    Boolean unreadOnly;

    String conversationType; // Conversation type
}
