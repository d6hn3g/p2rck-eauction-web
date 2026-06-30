package com.github.dghng36.eauction.e_auction_system.unit.modules.social.message.service;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.dghng36.eauction.modules.social.message.dto.internal.MessageReaction;
import com.github.dghng36.eauction.modules.social.message.mapper.MessageReactionMapper;
import com.github.dghng36.eauction.modules.social.message.model.Message;
import com.github.dghng36.eauction.modules.social.message.service.ReactionService;

@ExtendWith(MockitoExtension.class)
public class ReactionServiceTest {
    @Mock private MessageReactionMapper messageReactionMapper;

    @InjectMocks private ReactionService reactionService;

    private final String userId = "user-id-123";
    private final String otherUserId = "user-id-456";
    private final String emoji = "👍";
    private final String newEmoji = "❤️";
    private Message mockMessage;
    private MessageReaction existingReaction;
    private MessageReaction newReaction;

    @BeforeEach
    void setUp() {
        existingReaction = MessageReaction.builder()
            .userId(userId)
            .emoji(emoji)
            .build();

        newReaction = MessageReaction.builder()
            .userId(userId)
            .emoji(newEmoji)
            .build();

        mockMessage = Message.builder()
            .id("message-id-1")
            .conversationId("conversation-id-1")
            .senderId(otherUserId)
            .reactions(new ArrayList<>(List.of(existingReaction)))
            .build();
    }

    @Test
    void addOrUpdateReaction_AddNewReaction() {
        // Arrange
        Message messageWithoutReaction = Message.builder()
            .id("message-id-2")
            .reactions(new ArrayList<>())
            .build();
        when(messageReactionMapper.toMessageReactionEntity(userId, emoji)).thenReturn(existingReaction);

        // Act
        reactionService.addOrUpdateReaction(userId, messageWithoutReaction, emoji);

        // Assert
        assertEquals(1, messageWithoutReaction.getReactions().size());
        assertEquals(emoji, messageWithoutReaction.getReactions().get(0).getEmoji());
        verify(messageReactionMapper, times(1)).toMessageReactionEntity(userId, emoji);
    }

    @Test
    void addOrUpdateReaction_UpdateExistingReaction() {
        // Arrange
        when(messageReactionMapper.toMessageReactionEntity(userId, newEmoji)).thenReturn(newReaction);

        // Act
        reactionService.addOrUpdateReaction(userId, mockMessage, newEmoji);

        // Assert
        assertEquals(1, mockMessage.getReactions().size());
        assertEquals(newEmoji, mockMessage.getReactions().get(0).getEmoji());
        verify(messageReactionMapper, times(1)).toMessageReactionEntity(userId, newEmoji);
    }

    @Test
    void addOrUpdateReaction_NullEmoji() {
        // Arrange
        Message messageWithoutReaction = Message.builder()
            .id("message-id-3")
            .reactions(new ArrayList<>())
            .build();
        MessageReaction nullEmojiReaction = MessageReaction.builder()
            .userId(userId)
            .emoji(null)
            .build();
        when(messageReactionMapper.toMessageReactionEntity(userId, null)).thenReturn(nullEmojiReaction);

        // Act
        reactionService.addOrUpdateReaction(userId, messageWithoutReaction, null);

        // Assert
        assertEquals(1, messageWithoutReaction.getReactions().size());
        assertTrue(messageWithoutReaction.getReactions().get(0).getEmoji() == null);
    }

    @Test
    void removeReaction_Success() {
        // Act
        reactionService.removeReaction(userId, mockMessage, emoji);

        // Assert
        assertTrue(mockMessage.getReactions().isEmpty());
    }

    @Test
    void removeReaction_ReactionNotFound() {
        // Arrange
        int sizeBefore = mockMessage.getReactions().size();

        // Act
        reactionService.removeReaction(userId, mockMessage, "🎉");

        // Assert — no reaction removed
        assertEquals(sizeBefore, mockMessage.getReactions().size());
    }
}
