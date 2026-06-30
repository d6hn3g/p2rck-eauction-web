package com.github.dghng36.eauction.e_auction_system.unit.modules.social.chat.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.bson.Document;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;

import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.modules.social.chat.model.ChatSession;
import com.github.dghng36.eauction.modules.social.chat.repository.ChatSessionRepository;
import com.github.dghng36.eauction.modules.social.chat.service.ChatSessionService;
import com.github.dghng36.eauction.modules.social.presence.service.InternalPresenceService;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

@ExtendWith(MockitoExtension.class)
public class ChatSessionServiceTest {
    @Mock MongoTemplate mongoTemplate;
    @Mock private ChatSessionRepository chatSessionRepo;
    @Mock private InternalPresenceService internalPresenceService;

    @InjectMocks private ChatSessionService chatSessionService;

    private final String userId = "user-id-123";
    private final String otherUserId = "user-id-456";
    private final String sessionId = "session-id-789";
    private ChatSession mockChatSession;

    @BeforeEach
    void setUp() {
        mockChatSession = ChatSession.builder()
            .id("chat-session-1")
            .userId(userId)
            .sessionId(sessionId)
            .connectedAt(Instant.now())
            .lastHeartbeatAt(Instant.now())
            .build();
    }

    @Test
    void heartbeat_SessionExists_ShouldUpdateHeartbeatSuccessfully() {
        // Arrange
        UpdateResult mockUpdateResult = UpdateResult.acknowledged(1L, 1L, null);
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(ChatSession.class)))
            .thenReturn(mockUpdateResult);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);

        // Act & Assert
        assertDoesNotThrow(() -> chatSessionService.heartbeat(userId, sessionId));

        verify(mongoTemplate, times(1)).updateFirst(queryCaptor.capture(), updateCaptor.capture(), eq(ChatSession.class));
        
        String queryJson = queryCaptor.getValue().getQueryObject().toJson();
        assertThat(queryJson).contains(sessionId).contains(userId);
        
        Document updateDoc = updateCaptor.getValue().getUpdateObject();
        assertTrue(updateDoc.containsKey("$set"));
        assertTrue(((Document) updateDoc.get("$set")).containsKey("lastHeartbeatAt"));
    }

    @Test
    void heartbeat_WithNoSessionExists_ShouldThrowNotFound() {
        // Arrange — matchedCount = 0
        UpdateResult mockUpdateResult = UpdateResult.acknowledged(0L, 0L, null);
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(ChatSession.class)))
            .thenReturn(mockUpdateResult);

        // Act & Assert
        AppException ex = assertThrows(AppException.class, () ->
            chatSessionService.heartbeat(userId, sessionId));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("Chat session not found or unauthorized", ex.getMessage());
    }

    @Test
    void connect_ShouldInsertNewChatSession() {
        // Arrange
        ArgumentCaptor<ChatSession> captor = ArgumentCaptor.forClass(ChatSession.class);
        when(mongoTemplate.insert(captor.capture())).thenReturn(mockChatSession);

        // Act
        chatSessionService.connect(userId, sessionId);

        // Assert
        ChatSession saved = captor.getValue();
        assertEquals(userId, saved.getUserId());
        assertEquals(sessionId, saved.getSessionId());
        assertNotNull(saved.getConnectedAt());
        assertNotNull(saved.getLastHeartbeatAt());
    }

    @Test
    void disconnect_SessionExists_ShouldRemoveSuccessfully() {
        // Arrange
        DeleteResult mockDeleteResult = DeleteResult.acknowledged(1L);
        when(mongoTemplate.remove(any(Query.class), eq(ChatSession.class))).thenReturn(mockDeleteResult);
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);

        // Act
        assertDoesNotThrow(() -> chatSessionService.disconnect(userId, sessionId));

        // Assert
        verify(mongoTemplate, times(1)).remove(queryCaptor.capture(), eq(ChatSession.class));
        String queryJson = queryCaptor.getValue().getQueryObject().toJson();
        assertThat(queryJson).contains(sessionId).contains(userId);
    }

    @Test
    void disconnect_SessionDoesNotExist_ShouldThrowNotFound() {
        // Arrange — deletedCount = 0
        DeleteResult mockDeleteResult = DeleteResult.acknowledged(0L);
        when(mongoTemplate.remove(any(Query.class), eq(ChatSession.class))).thenReturn(mockDeleteResult);

        // Act & Assert
        AppException ex = assertThrows(AppException.class, () ->
            chatSessionService.disconnect(userId, sessionId));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("Chat session not found or unauthorized", ex.getMessage());
    }

    @Test
    void isOnline_WithNoSessionExists_ShouldReturnFalse() {
        when(chatSessionRepo.existsByUserIdAndLastHeartbeatAtAfter(eq(userId), any(Instant.class)))
            .thenReturn(false);

        boolean result = chatSessionService.isOnline(userId);
        assertFalse(result);
    }

    @Test
    void isOnline_WithSessionExistsAndUserIdMatches_ShouldReturnTrue() {
        when(chatSessionRepo.existsByUserIdAndLastHeartbeatAtAfter(eq(userId), any(Instant.class)))
            .thenReturn(true);

        boolean result = chatSessionService.isOnline(userId);
        assertTrue(result);
    }

    @Test
    void getChatSession_Exists_ShouldReturnSession() {
        when(chatSessionRepo.findBySessionId(sessionId)).thenReturn(Optional.of(mockChatSession));

        ChatSession result = chatSessionService.getChatSession(sessionId);
        assertNotNull(result);
        assertEquals(sessionId, result.getSessionId());
    }

    @Test
    void getChatSession_NotExists_ShouldThrowNotFound() {
        when(chatSessionRepo.findBySessionId(sessionId)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> chatSessionService.getChatSession(sessionId));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("Chat session not found", ex.getMessage());
    }

    @Test
    void cleanUpExpiredChatSessions_WithNoExpiredSessions_ShouldDoNothing() {
        // Arrange
        when(mongoTemplate.find(any(Query.class), eq(ChatSession.class))).thenReturn(new ArrayList<>());

        // Act
        chatSessionService.cleanUpExpiredChatSessions();

        // Assert
        verify(mongoTemplate, never()).remove(any(Query.class), eq(ChatSession.class));
        verifyNoInteractions(internalPresenceService, chatSessionRepo);
    }

    @Test
    void cleanUpExpiredChatSessions_WithExpiredSessions_ShouldDeleteAndMarkOfflineIfTrulyDisconnected() {
        // Arrange
        ChatSession expired1 = ChatSession.builder().userId("user-expired-1").build();
        ChatSession expired2 = ChatSession.builder().userId("user-expired-2").build();
        
        when(mongoTemplate.find(any(Query.class), eq(ChatSession.class))).thenReturn(List.of(expired1, expired2));
        when(mongoTemplate.remove(any(Query.class), eq(ChatSession.class))).thenReturn(DeleteResult.acknowledged(2L));
        
        when(chatSessionRepo.existsByUserIdAndLastHeartbeatAtAfter(eq("user-expired-1"), any(Instant.class))).thenReturn(false);
        when(chatSessionRepo.existsByUserIdAndLastHeartbeatAtAfter(eq("user-expired-2"), any(Instant.class))).thenReturn(true);

        // Act
        chatSessionService.cleanUpExpiredChatSessions();

        // Assert
        verify(mongoTemplate, times(1)).remove(any(Query.class), eq(ChatSession.class));
        
        verify(internalPresenceService, times(1)).markUserOffline("user-expired-1");
        verify(internalPresenceService, never()).markUserOffline("user-expired-2");
    }
}