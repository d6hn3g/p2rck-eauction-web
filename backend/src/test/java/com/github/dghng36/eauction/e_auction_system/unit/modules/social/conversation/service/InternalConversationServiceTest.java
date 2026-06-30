package com.github.dghng36.eauction.e_auction_system.unit.modules.social.conversation.service;

import java.time.Instant;
import java.util.List;

import static org.junit.Assert.assertTrue;
import org.junit.jupiter.api.Assertions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;

import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.modules.social.conversation.mapper.ConversationMapper;
import com.github.dghng36.eauction.modules.social.conversation.model.Conversation;
import com.github.dghng36.eauction.modules.social.conversation.repository.ConversationRepository;
import com.github.dghng36.eauction.modules.social.conversation.service.ConversationParticipantService;
import com.github.dghng36.eauction.modules.social.conversation.service.InternalConversationService;
import com.github.dghng36.eauction.modules.social.enums.ConversationType;
import com.mongodb.client.result.UpdateResult;

@ExtendWith(MockitoExtension.class)
public class InternalConversationServiceTest {
    @Mock private MongoTemplate mongoTemplate;
    @Mock private ConversationRepository conversationRepo;
    @Mock private ConversationParticipantService conversationParticipantService;
    @Mock private ConversationMapper conversationMapper;

    @InjectMocks private InternalConversationService internalConversationService;

    private final String conversationId = "conversation-id-123";
    private final String auctionRoomId = "auction-room-id-456";
    private final String creatorUserId = "creator-user-id-789";
    private final String senderId = "sender-id-111";
    private final String recipientId = "recipient-id-222";
    private final String messageId = "message-id-333";
    private Conversation mockConversation;

    @BeforeEach
    void setUp() {
        mockConversation = Conversation.builder()
            .id(conversationId)
            .title("Auction Room Chat")
            .type(ConversationType.AUCTION_ROOM)
            .participantIds(List.of(senderId, recipientId))
            .auctionRoomId(auctionRoomId)
            .active(true)
            .isDeleted(false)
            .build();
    }

    @Test
    void createAuctionRoomConversation_Success() {
        // Arrange
        when(conversationRepo.existsByAuctionRoomIdAndIsDeletedFalse(auctionRoomId)).thenReturn(false);
        when(conversationMapper.toAuctionRoomConversationEntity(auctionRoomId, "Auction Room Chat", creatorUserId))
            .thenReturn(mockConversation);
        when(conversationRepo.save(mockConversation)).thenReturn(mockConversation);

        // Act
        String result = internalConversationService.createAuctionRoomConversation(
            auctionRoomId, "Auction Room Chat", creatorUserId);

        // Assert
        assertEquals(conversationId, result);
        verify(conversationRepo, times(1)).save(mockConversation);
    }

    @Test
    void createAuctionRoomConversation_Conflict() {
        // Arrange
        when(conversationRepo.existsByAuctionRoomIdAndIsDeletedFalse(auctionRoomId)).thenReturn(true);

        // Act & Assert
        AppException ex = assertThrows(AppException.class, () ->
            internalConversationService.createAuctionRoomConversation(
                auctionRoomId, "Auction Room Chat", creatorUserId));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(conversationRepo, never()).save(any(Conversation.class));
    }

    @Test
    void archiveAuctionRoomConversation_Success() {
        // Arrange
        UpdateResult mockResult = UpdateResult.acknowledged(1L, 1L, null);
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Conversation.class)))
            .thenReturn(mockResult);

        // Act
        internalConversationService.archiveAuctionRoomConversation(conversationId);

        // Assert
        verify(mongoTemplate, times(1)).updateFirst(any(Query.class), any(Update.class), eq(Conversation.class));
    }

    @Test
    void archiveAuctionRoomConversation_NotFound() {
        // Arrange
        UpdateResult mockResult = UpdateResult.acknowledged(0L, 0L, null);
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Conversation.class)))
            .thenReturn(mockResult);

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () ->
            internalConversationService.archiveAuctionRoomConversation(conversationId));
            
        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("Conversation not found"));
    }

    @Test
    void unarchiveAuctionRoomConversation_Success() {
        // Arrange
        UpdateResult mockResult = UpdateResult.acknowledged(1L, 1L, null);
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Conversation.class)))
            .thenReturn(mockResult);

        // Act
        internalConversationService.unarchiveAuctionRoomConversation(conversationId);

        // Assert
        verify(mongoTemplate, times(1)).updateFirst(any(Query.class), any(Update.class), eq(Conversation.class));
    }

    @Test
    void unarchiveAuctionRoomConversation_NotFound() {
        // Arrange
        UpdateResult mockResult = UpdateResult.acknowledged(0L, 0L, null);
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Conversation.class)))
            .thenReturn(mockResult);

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () ->
            internalConversationService.unarchiveAuctionRoomConversation(conversationId));

        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("Conversation not found"));
    }

    @Test
    void updateLastMessage_Success() {
        // Arrange
        Instant messageTime = Instant.now();
        UpdateResult mockResult = UpdateResult.acknowledged(1L, 1L, null);
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Conversation.class)))
            .thenReturn(mockResult);
        doNothing().when(conversationParticipantService)
            .markParticipantAsRead(senderId, conversationId, messageId);

        // Act
        internalConversationService.updateLastMessage(
            senderId, conversationId, messageId, "Hello world", messageTime);

        // Assert
        verify(mongoTemplate, times(1)).updateFirst(any(Query.class), any(Update.class), eq(Conversation.class));
        verify(conversationParticipantService, times(1))
            .markParticipantAsRead(senderId, conversationId, messageId);
    }

    @Test
    void updateLastMessage_NotFound() {
        // Arrange
        Instant messageTime = Instant.now();
        UpdateResult mockResult = UpdateResult.acknowledged(0L, 0L, null);
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Conversation.class)))
            .thenReturn(mockResult);

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () ->
            internalConversationService.updateLastMessage(
                senderId, conversationId, messageId, "Hello world", messageTime));

        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("Conversation not found"));
    }

    @Test
    void updateLastMessage_Forbidden() {
        // Act — blank content causes early return without update
        internalConversationService.updateLastMessage(
            senderId, conversationId, messageId, "", Instant.now());

        // Assert
        verify(mongoTemplate, never()).updateFirst(any(Query.class), any(Update.class), eq(Conversation.class));
        verify(conversationParticipantService, never())
            .markParticipantAsRead(any(), any(), any());
    }

    @Test
    void incrementUnreadCount_ShouldDelegateToParticipantService() {
        // Arrange
        
        // Act & Assert
        Assertions.assertDoesNotThrow(() -> 
            internalConversationService.incrementUnreadCount(senderId, conversationId)
        );

        // Verify
        verify(conversationParticipantService, times(1))
            .incrementUnreadCountForAllExceptSender(senderId, conversationId);
    }
}
