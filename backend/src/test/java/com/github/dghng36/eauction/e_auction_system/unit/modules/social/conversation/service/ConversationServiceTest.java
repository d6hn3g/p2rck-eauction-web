package com.github.dghng36.eauction.e_auction_system.unit.modules.social.conversation.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;

import com.github.dghng36.eauction.core.base.PageResponse;
import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.e_auction_system.unit.support.JobExecutorTasksMockHelper;
import com.github.dghng36.eauction.infra.config.async.JobExecutorTasks;
import com.github.dghng36.eauction.modules.identity.user.service.InternalUserService;
import com.github.dghng36.eauction.modules.social.conversation.dto.request.CreateDirectConversationRequest;
import com.github.dghng36.eauction.modules.social.conversation.dto.response.ConversationResponse;
import com.github.dghng36.eauction.modules.social.conversation.mapper.ConversationMapper;
import com.github.dghng36.eauction.modules.social.conversation.model.Conversation;
import com.github.dghng36.eauction.modules.social.conversation.model.ConversationParticipant;
import com.github.dghng36.eauction.modules.social.conversation.repository.ConversationRepository;
import com.github.dghng36.eauction.modules.social.conversation.service.ConversationParticipantService;
import com.github.dghng36.eauction.modules.social.conversation.service.ConversationService;
import com.github.dghng36.eauction.modules.social.enums.ConversationType;

@ExtendWith(MockitoExtension.class)
public class ConversationServiceTest {
    @Mock private MongoTemplate mongoTemplate;
    @Mock private ConversationRepository conversationRepo;
    @Mock private InternalUserService internalUserService;
    @Mock private ConversationParticipantService conversationParticipantService;
    @Mock private ConversationMapper conversationMapper;
    @Mock private JobExecutorTasks jobExecutorTasks;

    @InjectMocks private ConversationService conversationService;

    private final String userId = "user-id-123";
    private final String recipientUserId = "recipient-user-id-456";
    private final String conversationId = "conversation-id-789";
    private Conversation mockConversation;
    private ConversationParticipant mockParticipant;
    private ConversationResponse mockConversationResponse;

    @BeforeEach
    void setUp() {
        JobExecutorTasksMockHelper.runSynchronously(jobExecutorTasks);

        mockConversation = Conversation.builder()
            .id(conversationId)
            .type(ConversationType.DIRECT)
            .participantIds(List.of(userId, recipientUserId))
            .lastMessageTime(Instant.now())
            .build();

        mockParticipant = ConversationParticipant.builder()
            .id("participant-id-1")
            .userId(userId)
            .conversationId(conversationId)
            .unreadCount(0)
            .build();

        mockConversationResponse = ConversationResponse.builder()
            .id(conversationId)
            .type("DIRECT")
            .unreadCount(0)
            .build();
    }

    @Test
    void createDirectConversation_validUsers_shouldCreateConversation() {
        // Arrange
        CreateDirectConversationRequest request = CreateDirectConversationRequest.builder()
            .recipientUserId(recipientUserId)
            .build();
        when(conversationRepo.findDirectConversationBetweenTwoUsersAndIsDeletedFalse(userId, recipientUserId))
            .thenReturn(Optional.empty());
        when(conversationMapper.toDirectConversationEntity(userId, recipientUserId))
            .thenReturn(mockConversation);
        when(conversationRepo.save(mockConversation)).thenReturn(mockConversation);
        doNothing().when(conversationParticipantService).createParticipant(any(), any());
        when(conversationMapper.toConversationResponse(mockConversation))
            .thenReturn(mockConversationResponse);

        // Act
        ConversationResponse result = conversationService.createDirectConversation(userId, request);

        // Assert
        assertNotNull(result);
        assertEquals(conversationId, result.getId());
        verify(conversationParticipantService, times(2)).createParticipant(any(), eq(conversationId));
        verify(conversationRepo, times(1)).save(mockConversation);
    }

    @Test
    void createDirectConversation_sameUser_shouldThrowException() {
        // Arrange
        CreateDirectConversationRequest request = CreateDirectConversationRequest.builder()
            .recipientUserId(userId)
            .build();

        // Act & Assert
        AppException ex = assertThrows(AppException.class, () ->
            conversationService.createDirectConversation(userId, request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(conversationRepo, never()).save(any(Conversation.class));
    }

    @Test
    void createDirectConversation_nonExistentUser_shouldThrowException() {
        // Arrange — creating conversation does not validate recipient existence;
        // simulate failure during participant creation
        CreateDirectConversationRequest request = CreateDirectConversationRequest.builder()
            .recipientUserId(recipientUserId)
            .build();
        when(conversationRepo.findDirectConversationBetweenTwoUsersAndIsDeletedFalse(userId, recipientUserId))
            .thenReturn(Optional.empty());
        when(conversationMapper.toDirectConversationEntity(userId, recipientUserId))
            .thenReturn(mockConversation);
        when(conversationRepo.save(mockConversation)).thenReturn(mockConversation);
        doNothing().when(conversationParticipantService).createParticipant(eq(userId), eq(conversationId));
        org.mockito.Mockito.doThrow(new AppException("User not found", HttpStatus.NOT_FOUND))
            .when(conversationParticipantService).createParticipant(eq(recipientUserId), eq(conversationId));

        // Act & Assert
        AppException ex = assertThrows(AppException.class, () ->
            conversationService.createDirectConversation(userId, request));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void createDirectConversation_existingConversation_shouldReturnExistingConversation() {
        // Arrange
        CreateDirectConversationRequest request = CreateDirectConversationRequest.builder()
            .recipientUserId(recipientUserId)
            .build();
        when(conversationRepo.findDirectConversationBetweenTwoUsersAndIsDeletedFalse(userId, recipientUserId))
            .thenReturn(Optional.of(mockConversation));
        when(conversationParticipantService.getParticipant(userId, conversationId))
            .thenReturn(mockParticipant);
        when(conversationMapper.toConversationResponse(eq(mockConversation), eq(0), eq(false), eq(false), eq(false)))
            .thenReturn(mockConversationResponse);

        // Act
        ConversationResponse result = conversationService.createDirectConversation(userId, request);

        // Assert
        assertNotNull(result);
        assertEquals(conversationId, result.getId());
        verify(conversationRepo, never()).save(any(Conversation.class));
    }

    @Test
    void createDirectConversation_ValidCreateParticipantConcurrency() {
        // Arrange
        CreateDirectConversationRequest request = CreateDirectConversationRequest.builder()
            .recipientUserId(recipientUserId)
            .build();
        when(conversationRepo.findDirectConversationBetweenTwoUsersAndIsDeletedFalse(userId, recipientUserId))
            .thenReturn(Optional.empty());
        when(conversationMapper.toDirectConversationEntity(userId, recipientUserId))
            .thenReturn(mockConversation);
        when(conversationRepo.save(mockConversation)).thenReturn(mockConversation);
        doNothing().when(conversationParticipantService).createParticipant(any(), any());
        when(conversationMapper.toConversationResponse(mockConversation))
            .thenReturn(mockConversationResponse);

        // Act
        conversationService.createDirectConversation(userId, request);

        // Assert — both participants created concurrently
        verify(conversationParticipantService).createParticipant(userId, conversationId);
        verify(conversationParticipantService).createParticipant(recipientUserId, conversationId);
    }

    @Test
    void getUserConversations_validUser_shouldReturnConversations() {
        // Arrange
        Page<Conversation> conversationPage = new PageImpl<>(
            List.of(mockConversation),
            PageRequest.of(0, 10),
            1
        );
        when(conversationRepo.findActiveConversationsByUserId(eq(userId), any(Pageable.class)))
            .thenReturn(conversationPage);
        when(conversationParticipantService.getParticipantsByUserIdAndConversationIds(userId, List.of(conversationId)))
            .thenReturn(List.of(mockParticipant));
        when(conversationMapper.toConversationResponse(eq(mockConversation), eq(0), eq(false), eq(false), eq(false)))
            .thenReturn(mockConversationResponse);

        // Act
        PageResponse<ConversationResponse> result = conversationService.getUserConversations(
            userId, 0, 10, "lastMessageTime", "desc");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getData().size());
        assertEquals(conversationId, result.getData().get(0).getId());
    }

    @Test
    void getUserConversations_nonExistentUser_shouldThrowException() {
        // Arrange — empty page for user with no conversations
        Page<Conversation> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(conversationRepo.findActiveConversationsByUserId(eq(userId), any(Pageable.class)))
            .thenReturn(emptyPage);

        // Act
        PageResponse<ConversationResponse> result = conversationService.getUserConversations(
            userId, 0, 10, "lastMessageTime", "desc");

        // Assert — returns empty list, not an exception
        assertNotNull(result);
        assertEquals(0, result.getData().size());
    }

    @Test
    void getUserConversations_WithSortByOfConversationParticipant_shouldReturnSortedConversations() {
        // Arrange
        Page<ConversationParticipant> participantPage = new PageImpl<>(
            List.of(mockParticipant),
            PageRequest.of(0, 10),
            1
        );
        when(conversationParticipantService.getParticipantsByUserId(eq(userId), any(Pageable.class)))
            .thenReturn(participantPage);
        when(conversationRepo.findByIdInAndIsDeletedFalse(List.of(conversationId)))
            .thenReturn(List.of(mockConversation));
        when(conversationMapper.toConversationResponse(eq(mockConversation), eq(0), eq(false), eq(false), eq(false)))
            .thenReturn(mockConversationResponse);

        // Act
        PageResponse<ConversationResponse> result = conversationService.getUserConversations(
            userId, 0, 10, "unreadCount", "desc");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getData().size());
        verify(conversationParticipantService, times(1)).getParticipantsByUserId(eq(userId), any(Pageable.class));
    }

    @Test
    void getUserConversations_WithSortByOfConversation_shouldReturnSortedConversations() {
        // Arrange
        Page<Conversation> conversationPage = new PageImpl<>(
            List.of(mockConversation),
            PageRequest.of(0, 10),
            1
        );
        when(conversationRepo.findActiveConversationsByUserId(eq(userId), any(Pageable.class)))
            .thenReturn(conversationPage);
        when(conversationParticipantService.getParticipantsByUserIdAndConversationIds(userId, List.of(conversationId)))
            .thenReturn(List.of(mockParticipant));
        when(conversationMapper.toConversationResponse(eq(mockConversation), eq(0), eq(false), eq(false), eq(false)))
            .thenReturn(mockConversationResponse);

        // Act
        PageResponse<ConversationResponse> result = conversationService.getUserConversations(
            userId, 0, 10, "title", "asc");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getData().size());
        verify(conversationRepo, times(1)).findActiveConversationsByUserId(eq(userId), any(Pageable.class));
    }
}
