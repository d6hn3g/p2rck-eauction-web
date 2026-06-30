package com.github.dghng36.eauction.modules.social.message.mapper;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.github.dghng36.eauction.modules.social.message.dto.internal.MessageReaction;
import com.github.dghng36.eauction.modules.social.message.dto.response.MessageReactionResponse;

@Component
public class MessageReactionMapper {
    public MessageReaction toMessageReactionEntity(String userId, String emoji) {
        return MessageReaction.builder()
            .userId(userId)
            .emoji(emoji)
            .build();
    }

    private MessageReactionResponse toMessageReactionResponse(
        String emoji,
        int count,
        boolean reacted
    ) {
        return MessageReactionResponse.builder()
            .emoji(emoji)
            .count(count)
            .reacted(reacted)
            .build();
    }

    public List<MessageReactionResponse> toMessageReactionResponseList(String userId, List<MessageReaction> reactions) {
        if (reactions == null) {
            return Collections.emptyList();
        }

        Map<String, List<MessageReaction>> groupedReactions = reactions.stream()
            .collect(
                Collectors.groupingBy(MessageReaction::getEmoji)
            );

        return groupedReactions.entrySet().stream()
            .map(entry -> {
                String emoji = entry.getKey();
                List<MessageReaction> reactionList = entry.getValue();
                int count = reactionList.size();
                boolean reacted = reactionList.stream().anyMatch(r -> r.getUserId().equals(userId));
                return this.toMessageReactionResponse(emoji, count, reacted);
            })
            .sorted(
                Comparator.comparing(MessageReactionResponse::getReacted)
                    .thenComparing(Comparator.comparing(MessageReactionResponse::getCount)
                    .reversed()
                )
            )
            .toList();
    }


}
