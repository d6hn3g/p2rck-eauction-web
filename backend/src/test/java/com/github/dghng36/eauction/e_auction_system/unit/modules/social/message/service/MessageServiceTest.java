package com.github.dghng36.eauction.e_auction_system.unit.modules.social.message.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;

import com.github.dghng36.eauction.core.base.PageResponse;
import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.e_auction_system.unit.support.JobExecutorTasksMockHelper;
import com.github.dghng36.eauction.infra.config.async.JobExecutorTasks;
import com.github.dghng36.eauction.modules.identity.user.dto.internal.UserInfo;
import com.github.dghng36.eauction.modules.identity.user.service.InternalUserService;
import com.github.dghng36.eauction.modules.social.chat.dto.internal.ChatSocketMessage;
import com.github.dghng36.eauction.modules.social.conversation.service.InternalConversationService;
import com.github.dghng36.eauction.modules.social.enums.MessageType;
import com.github.dghng36.eauction.modules.social.message.dto.request.SearchMessagesRequest;
import com.github.dghng36.eauction.modules.social.message.dto.response.MessageResponse;
import com.github.dghng36.eauction.modules.social.message.event.MessageSentEvent;
import com.github.dghng36.eauction.modules.social.message.mapper.MessageMapper;
import com.github.dghng36.eauction.modules.social.message.model.Message;
import com.github.dghng36.eauction.modules.social.message.repository.MessageRepository;
import com.github.dghng36.eauction.modules.social.message.service.InternalMessageService;
import com.github.dghng36.eauction.modules.social.message.service.MessageService;
import com.github.dghng36.eauction.modules.social.message.service.ReactionService;

@ExtendWith(MockitoExtension.class)
public class MessageServiceTest {
    @Mock private MongoTemplate mongoTemplate;
    @Mock private MessageRepository messageRepo;
    @Mock private InternalUserService internalUserService;
    @Mock private InternalConversationService internalConversationService;
    @Mock private ReactionService reactionService;
    @Mock private InternalMessageService internalMessageService;
    @Mock private MessageMapper messageMapper;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private JobExecutorTasks jobExecutorTasks;

    @InjectMocks private MessageService messageService;

    private final String userId = "user-id-123";
    private final String otherUserId = "other-user-id-456";
    private final String conversationId = "conversation-id-789";
    private final String messageId = "message-id-111";
    private Message mockMessage;
    private MessageResponse mockMessageResponse;
    private UserInfo mockUserInfo;

    @BeforeEach
    void setUp() {
        JobExecutorTasksMockHelper.runSynchronously(jobExecutorTasks);

        mockUserInfo = UserInfo.builder()
            .id(userId)
            .username("testuser")
            .build();

        mockMessage = Message.builder()
            .id(messageId)
            .conversationId(conversationId)
            .senderId(userId)
            .content("Hello world")
            .type(MessageType.TEXT)
            .createdAt(Instant.now())
            .deletedForUsers(new ArrayList<>())
            .reactions(new ArrayList<>())
            .build();

        mockMessageResponse = MessageResponse.builder()
            .id(messageId)
            .conversationId(conversationId)
            .content("Hello world")
            .messageType("TEXT")
            .build();

    }

    @Test
    void sendMessage_ValidData_ShouldReturnSuccessAndPublishEvent() {
        // Arrange
        String textContent = "Hello world";
        
        ChatSocketMessage realChatSocketMessage = ChatSocketMessage.builder()
            .conversationId(conversationId)
            .content(textContent)
            .type(MessageType.TEXT)
            .attachments(null)
            .replyToMessageId(null)
            .build();

        Message realMessage = Message.builder()
            .id(messageId)
            .conversationId(conversationId)
            .content(textContent)
            .createdAt(Instant.now())
            .senderId(userId)
            .build();

        doNothing().when(internalConversationService).validateActive(conversationId);
        doNothing().when(internalConversationService).validateParticipant(userId, conversationId);

        when(messageMapper.toMessageEntity(
            eq(userId), eq(conversationId), eq(textContent), eq("TEXT"), any(), eq(null)))
            .thenReturn(realMessage);
        
        when(internalMessageService.saveMessageIndependent(realMessage)).thenReturn(realMessage);

        when(internalUserService.getUserInfoByIds(Set.of(userId))).thenReturn(Map.of(userId, mockUserInfo));
        when(messageMapper.toMessageResponse(eq(realMessage), eq(mockUserInfo), any()))
            .thenReturn(mockMessageResponse);

        // Act
        MessageResponse result = messageService.sendMessage(userId, realChatSocketMessage);

        // Assert
        assertNotNull(result);
        assertEquals(messageId, result.getId());
        assertEquals("Hello world", result.getContent());
        verify(internalMessageService, times(1)).saveMessageIndependent(realMessage);
        verify(eventPublisher, times(1)).publishEvent(any(MessageSentEvent.class));
    }

    @Test
    void sendMessages_ConversationNotFound() {
        // Arrange
        ChatSocketMessage realChatSocketMessage = ChatSocketMessage.builder()
            .conversationId(conversationId)
            .build();

        doThrow(new AppException("Conversation not found", HttpStatus.NOT_FOUND))
            .when(internalConversationService).validateActive(conversationId);

        // Act & Assert
        AppException ex = assertThrows(AppException.class, () ->
            messageService.sendMessage(userId, realChatSocketMessage));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        
        verify(internalMessageService, never()).saveMessageIndependent(any(Message.class));
    }

    @Test
    void sendMessages_UserNotInConversation() {
        // Arrange
        ChatSocketMessage realChatSocketMessage = ChatSocketMessage.builder()
            .conversationId(conversationId)
            .build();

        doNothing().when(internalConversationService).validateActive(conversationId);
        doThrow(new AppException("User is not a participant of this conversation", HttpStatus.FORBIDDEN))
            .when(internalConversationService).validateParticipant(userId, conversationId);

        // Act & Assert
        AppException ex = assertThrows(AppException.class, () ->
            messageService.sendMessage(userId, realChatSocketMessage));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        
        verify(internalMessageService, never()).saveMessageIndependent(any(Message.class));
    }

    @Test
    void sendMessages_InvalidMessageContent() {
        // Arrange — reply message belongs to a different conversation
        String replyMessageId = "reply-message-id";
        ChatSocketMessage replyMessage = ChatSocketMessage.builder()
            .conversationId(conversationId)
            .content("Reply content")
            .type(MessageType.TEXT)
            .replyToMessageId(replyMessageId)
            .build();
            
        Message replyToMessage = Message.builder()
            .id(replyMessageId)
            .conversationId("other-conversation-id")
            .senderId(otherUserId)
            .build();

        doNothing().when(internalConversationService).validateActive(conversationId);
        doNothing().when(internalConversationService).validateParticipant(userId, conversationId);
        
        when(messageRepo.findByIdAndIsDeletedFalse(replyMessageId))
            .thenReturn(Optional.of(replyToMessage));

        // Act & Assert
        AppException ex = assertThrows(AppException.class, () ->
            messageService.sendMessage(userId, replyMessage));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Reply message must be in the same conversation", ex.getMessage());
        
        verify(internalMessageService, never()).saveMessageIndependent(any(Message.class));
    }

    @Test
    void searchMessages_Success() {
        // Arrange
        SearchMessagesRequest request = SearchMessagesRequest.builder()
            .keyword("hello")
            .build();
        when(mongoTemplate.count(any(), eq(Message.class))).thenReturn(1L);
        when(mongoTemplate.find(any(), eq(Message.class))).thenReturn(List.of(mockMessage));
        when(internalUserService.getUserInfoByIds(anySet())).thenReturn(Map.of(userId, mockUserInfo));
        when(messageMapper.toMessageResponse(eq(mockMessage), eq(mockUserInfo), eq(null)))
            .thenReturn(mockMessageResponse);

        // Act
        PageResponse<MessageResponse> result = messageService.searchMessages(
            userId, conversationId, request, 0, 10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getData().size());
        assertEquals(messageId, result.getData().get(0).getId());
    }

    @Test
    void searchMessages_ConversationNotFound() {
        // Arrange — no messages found for the conversation
        SearchMessagesRequest request = SearchMessagesRequest.builder().build();
        when(mongoTemplate.count(any(), eq(Message.class))).thenReturn(0L);
        when(mongoTemplate.find(any(), eq(Message.class))).thenReturn(Collections.emptyList());

        // Act
        PageResponse<MessageResponse> result = messageService.searchMessages(
            userId, conversationId, request, 0, 10);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getData().size());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void searchMessages_UserNotInConversation() {
        // Arrange — messages deleted for this user are filtered out in response mapping
        SearchMessagesRequest request = SearchMessagesRequest.builder().build();
        Message deletedForUserMessage = Message.builder()
            .id("deleted-msg-id")
            .conversationId(conversationId)
            .senderId(otherUserId)
            .content("Hidden message")
            .type(MessageType.TEXT)
            .deletedForUsers(new ArrayList<>(List.of(userId)))
            .reactions(new ArrayList<>())
            .build();
        when(mongoTemplate.count(any(), eq(Message.class))).thenReturn(1L);
        when(mongoTemplate.find(any(), eq(Message.class))).thenReturn(List.of(deletedForUserMessage));
        when(internalUserService.getUserInfoByIds(anySet())).thenReturn(Map.of(otherUserId, mockUserInfo));

        // Act
        PageResponse<MessageResponse> result = messageService.searchMessages(
            userId, conversationId, request, 0, 10);

        // Assert — message filtered out for this user
        assertNotNull(result);
        assertEquals(0, result.getData().size());
    }

    @Test
    void searchMessages_InvalidSearchCriteria() {
        // Arrange
        SearchMessagesRequest request = SearchMessagesRequest.builder()
            .messageType("INVALID_TYPE")
            .build();

        // Act & Assert
        AppException ex = assertThrows(AppException.class, () ->
            messageService.searchMessages(userId, conversationId, request, 0, 10));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void searchMessages_NoResultsFound() {
        // Arrange
        SearchMessagesRequest request = SearchMessagesRequest.builder()
            .keyword("nonexistent-keyword")
            .build();
        when(mongoTemplate.count(any(), eq(Message.class))).thenReturn(0L);
        when(mongoTemplate.find(any(), eq(Message.class))).thenReturn(Collections.emptyList());

        // Act
        PageResponse<MessageResponse> result = messageService.searchMessages(
            userId, conversationId, request, 0, 10);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertEquals(0, result.getData().size());
        assertEquals(0, result.getCurrentPage());
    }
}
