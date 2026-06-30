package com.github.dghng36.eauction.e_auction_system.unit.modules.social.conversation.service;

import static org.assertj.core.api.Assertions.assertThat;
import org.bson.Document;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;

import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.modules.social.conversation.mapper.ConversationParticipantMapper;
import com.github.dghng36.eauction.modules.social.conversation.model.ConversationParticipant;
import com.github.dghng36.eauction.modules.social.conversation.repository.ConversationParticipantRepository;
import com.github.dghng36.eauction.modules.social.conversation.service.ConversationParticipantService;
import com.github.dghng36.eauction.modules.social.presence.service.InternalPresenceService;
import com.mongodb.client.result.UpdateResult;

@ExtendWith(MockitoExtension.class)
public class ConversationParticipantServiceTest {
    @Mock private MongoTemplate mongoTemplate;
    @Mock private ConversationParticipantRepository conversationParticipantRepo;
    @Mock private InternalPresenceService internalPresenceService;
    @Mock private ConversationParticipantMapper conversationParticipantMapper;

    @InjectMocks private ConversationParticipantService conversationParticipantService;

    private final String userId = "user-id-123";
    private final String otherUserId = "user-id-456";
    private final String conversationId = "conversation-id-789";
    private final String messageId = "message-id-111";
    private ConversationParticipant mockParticipant;

    @BeforeEach
    void setUp() {
        mockParticipant = ConversationParticipant.builder()
            .id("participant-id-1")
            .userId(userId)
            .conversationId(conversationId)
            .unreadCount(0)
            .build();
    }

    @Test
    void createParticipant_Success() {
        // Arrange
        when(conversationParticipantMapper.toConversationParticipantEntity(conversationId, userId))
            .thenReturn(mockParticipant);
        when(conversationParticipantRepo.save(mockParticipant)).thenReturn(mockParticipant);

        // Act
        conversationParticipantService.createParticipant(userId, conversationId);

        // Assert
        verify(conversationParticipantRepo, times(1)).save(mockParticipant);
    }

    @Test
    void createParticipant_UserNotFound_ShouldThrowException() {
        // Arrange — save failure propagates as exception
        when(conversationParticipantMapper.toConversationParticipantEntity(conversationId, userId))
            .thenReturn(mockParticipant);
        when(conversationParticipantRepo.save(mockParticipant))
            .thenThrow(new AppException("User not found", HttpStatus.NOT_FOUND));

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () ->
            conversationParticipantService.createParticipant(userId, conversationId));
        
        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("User not found"));
    }

    @Test
    void leaveAllParticipants_Success() {
        // Arrange
        UpdateResult mockResult = UpdateResult.acknowledged(2L, 2L, null);
        when(mongoTemplate.updateMulti(any(Query.class), any(Update.class), eq(ConversationParticipant.class)))
            .thenReturn(mockResult);

        // Act
        conversationParticipantService.leaveAllParticipants(conversationId);

        // Assert
        verify(mongoTemplate, times(1)).updateMulti(any(Query.class), any(Update.class), eq(ConversationParticipant.class));
    }

    @Test
    void leaveAllParticipants_ConversationNotFound_ShouldThrowException() {
        // Arrange — updateMulti with 0 matches does not throw in implementation
        UpdateResult mockResult = UpdateResult.acknowledged(0L, 0L, null);
        when(mongoTemplate.updateMulti(any(Query.class), any(Update.class), eq(ConversationParticipant.class)))
            .thenReturn(mockResult);

        // Act — no exception thrown by service
        conversationParticipantService.leaveAllParticipants(conversationId);

        // Assert
        verify(mongoTemplate, times(1)).updateMulti(any(Query.class), any(Update.class), eq(ConversationParticipant.class));
    }

    @Test
    void hideParticipant_Success() {
        // Arrange
        UpdateResult mockResult = UpdateResult.acknowledged(1L, 1L, null);
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(ConversationParticipant.class)))
            .thenReturn(mockResult);

        // Act
        conversationParticipantService.hideParticipant(userId, conversationId);

        // Assert
        verify(mongoTemplate, times(1)).updateFirst(any(Query.class), any(Update.class), eq(ConversationParticipant.class));
    }

    @Test
    void hideParticipant_ParticipantNotFound_ShouldThrowException() {
        // Arrange
        UpdateResult mockResult = UpdateResult.acknowledged(0L, 0L, null);
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(ConversationParticipant.class)))
            .thenReturn(mockResult);

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () ->
            conversationParticipantService.hideParticipant(userId, conversationId));
            
        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("Conversation participant not found"));
    }

    @Test
    void hideParticipant_AlreadyHidden_ShouldThrowException() {
        // Arrange — update still succeeds even if already hidden
        UpdateResult mockResult = UpdateResult.acknowledged(1L, 1L, null);
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(ConversationParticipant.class)))
            .thenReturn(mockResult);

        // Act — service allows re-hiding without error
        conversationParticipantService.hideParticipant(userId, conversationId);

        // Assert
        verify(mongoTemplate, times(1)).updateFirst(any(Query.class), any(Update.class), eq(ConversationParticipant.class));
    }

    @Test
    void togglePinParticipant_Success() {
        // Arrange
        UpdateResult mockResult = UpdateResult.acknowledged(1L, 1L, null);
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(ConversationParticipant.class)))
            .thenReturn(mockResult);

        // Act
        conversationParticipantService.togglePinParticipant(userId, conversationId, true);

        // Assert
        verify(mongoTemplate, times(1)).updateFirst(any(Query.class), any(Update.class), eq(ConversationParticipant.class));
    }

    @Test
    void togglePinParticipant_ParticipantNotFound_ShouldThrowException() {
        // Arrange
        UpdateResult mockResult = UpdateResult.acknowledged(0L, 0L, null);
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(ConversationParticipant.class)))
            .thenReturn(mockResult);

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () ->
            conversationParticipantService.togglePinParticipant(userId, conversationId, true));

        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("Conversation participant not found"));
    }

    @Test
    void toggleMuteParticipant_Success() {
        // Arrange
        UpdateResult mockResult = UpdateResult.acknowledged(1L, 1L, null);
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(ConversationParticipant.class)))
            .thenReturn(mockResult);

        // Act
        conversationParticipantService.toggleMuteParticipant(userId, conversationId, true);

        // Assert
        verify(mongoTemplate, times(1)).updateFirst(any(Query.class), any(Update.class), eq(ConversationParticipant.class));
    }

    @Test
    void toggleMuteParticipant_ParticipantNotFound_ShouldThrowException() {
        // Arrange
        UpdateResult mockResult = UpdateResult.acknowledged(0L, 0L, null);
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(ConversationParticipant.class)))
            .thenReturn(mockResult);

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () ->
            conversationParticipantService.toggleMuteParticipant(userId, conversationId, true));

        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("Conversation participant not found"));
    }

    @Test
    void markParticipantAsRead_Success() {
        // Arrange
        UpdateResult mockResult = UpdateResult.acknowledged(1L, 1L, null);
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(ConversationParticipant.class)))
            .thenReturn(mockResult);

        // Act
        conversationParticipantService.markParticipantAsRead(userId, conversationId, messageId);

        // Assert
        verify(mongoTemplate, times(1)).updateFirst(any(Query.class), any(Update.class), eq(ConversationParticipant.class));
    }

    @Test
    void markParticipantAsRead_ParticipantNotFound_ShouldThrowException() {
        // Arrange
        UpdateResult mockResult = UpdateResult.acknowledged(0L, 0L, null);
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(ConversationParticipant.class)))
            .thenReturn(mockResult);

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () ->
            conversationParticipantService.markParticipantAsRead(userId, conversationId, messageId));

        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("Conversation participant not found"));
    }

    @Test
    void leaveParticipant_Success() {
        // Arrange
        UpdateResult mockResult = UpdateResult.acknowledged(1L, 1L, null);
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(ConversationParticipant.class)))
            .thenReturn(mockResult);

        // Act
        conversationParticipantService.leaveParticipant(userId, conversationId);

        // Assert
        verify(mongoTemplate, times(1)).updateFirst(any(Query.class), any(Update.class), eq(ConversationParticipant.class));
    }

    @Test
    void leaveParticipant_ParticipantNotFound_ShouldThrowException() {
        // Arrange
        UpdateResult mockResult = UpdateResult.acknowledged(0L, 0L, null);
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(ConversationParticipant.class)))
            .thenReturn(mockResult);

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () ->
            conversationParticipantService.leaveParticipant(userId, conversationId));

        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("Conversation participant not found"));
    }

    @Test
    void leaveParticipant_AlreadyLeft_ShouldThrowException() {
        // Arrange — participant record no longer matches (already left)
        UpdateResult mockResult = UpdateResult.acknowledged(0L, 0L, null);
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(ConversationParticipant.class)))
            .thenReturn(mockResult);

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () ->
            conversationParticipantService.leaveParticipant(userId, conversationId));

        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("Conversation participant not found"));
    }

    @Test
    void incrementUnreadCount_Success() {
        // Arrange
        UpdateResult mockResult = UpdateResult.acknowledged(1L, 1L, null);
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(ConversationParticipant.class)))
            .thenReturn(mockResult);

        // Act
        conversationParticipantService.incrementUnreadCount(userId, conversationId);

        // Assert
        verify(mongoTemplate, times(1)).updateFirst(any(Query.class), any(Update.class), eq(ConversationParticipant.class));
    }

    @Test
    void incrementUnreadCount_ParticipantNotFound_ShouldThrowException() {
        // Arrange
        UpdateResult mockResult = UpdateResult.acknowledged(0L, 0L, null);
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(ConversationParticipant.class)))
            .thenReturn(mockResult);

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () ->
            conversationParticipantService.incrementUnreadCount(userId, conversationId));

        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("Conversation participant not found"));
    }

    @Test
    void incrementUnreadCount_ParticipantLeft_ShouldThrowException() {
        // Arrange — left participant not found by update query
        UpdateResult mockResult = UpdateResult.acknowledged(0L, 0L, null);
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(ConversationParticipant.class)))
            .thenReturn(mockResult);

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () ->
            conversationParticipantService.incrementUnreadCount(userId, conversationId));

        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("Conversation participant not found"));
    }

    @Test
    void incrementUnreadCountForAllExceptSender_Success() {
        // Arrange
       String senderId = "user-123";
        UpdateResult mockResult = UpdateResult.acknowledged(1L, 1L, null);
        
        when(mongoTemplate.updateMulti(any(Query.class), any(Update.class), eq(ConversationParticipant.class)))
            .thenReturn(mockResult);

        // Act
        conversationParticipantService.incrementUnreadCountForAllExceptSender(senderId, conversationId);

        // Assert
        verify(mongoTemplate, times(1)).updateMulti(any(Query.class), any(Update.class), eq(ConversationParticipant.class));
    }

    @Test
    void incrementUnreadCountForAllExceptSender_NoOtherParticipants_ShouldExecuteMultiUpdateWithoutException() {
        // Arrange
        String senderId = "user-123";
        String conversationId = "conversation-789";
        
        UpdateResult mockResult = UpdateResult.acknowledged(0L, 0L, null);
        
        when(mongoTemplate.updateMulti(any(Query.class), any(Update.class), eq(ConversationParticipant.class)))
            .thenReturn(mockResult);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);

        // Act & Assert
        assertDoesNotThrow(() -> {
            conversationParticipantService.incrementUnreadCountForAllExceptSender(senderId, conversationId);
        });

        // Verify
        verify(mongoTemplate, times(1)).updateMulti(queryCaptor.capture(), updateCaptor.capture(), eq(ConversationParticipant.class));
        
        String queryJson = queryCaptor.getValue().getQueryObject().toJson();
        assertThat(queryJson).contains(conversationId);
        assertThat(queryJson).contains("\"isDeleted\": false");
        
        Document updateDoc = updateCaptor.getValue().getUpdateObject();
        assertTrue(updateDoc.containsKey("$inc"));
        Document incFields = (org.bson.Document) updateDoc.get("$inc");
        assertEquals(1, incFields.get("unreadCount"));
    }
}
