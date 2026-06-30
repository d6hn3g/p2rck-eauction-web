package com.github.dghng36.eauction.e_auction_system.unit.modules.social.chat.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.modules.social.chat.service.ChatAuthorizationService;
import com.github.dghng36.eauction.modules.social.conversation.service.InternalConversationService;

@ExtendWith(MockitoExtension.class)
public class ChatAuthorizationTest {
    @Mock private InternalConversationService internalConversationService;

    @InjectMocks private ChatAuthorizationService chatAuthorizationService;

    private final String userId = "user-id-123";
    private final String conversationId = "conversation-id-456";

    @BeforeEach
    void setUp() {}

    @Test
    void validateParticipant_validParticipant_shouldPass() {
        // Act & Assert
        assertDoesNotThrow(() -> chatAuthorizationService.validateParticipant(userId, conversationId));
        verify(internalConversationService, times(1)).validateParticipant(userId, conversationId);
    }

    @Test
    void validateParticipant_invalidParticipant_shouldThrowException() {
        // Arrange
        doThrow(new AppException("User is not a participant of this conversation", HttpStatus.FORBIDDEN))
            .when(internalConversationService).validateParticipant(eq(userId), eq(conversationId));

        // Act & Assert
        AppException ex = assertThrows(AppException.class, () ->
            chatAuthorizationService.validateParticipant(userId, conversationId));

        assert(ex.getMessage().contains("User is not a participant of this conversation")); 
        verify(internalConversationService, times(1)).validateParticipant(userId, conversationId);
    }
}
