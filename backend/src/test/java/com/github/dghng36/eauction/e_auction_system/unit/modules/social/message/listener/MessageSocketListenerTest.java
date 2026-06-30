package com.github.dghng36.eauction.e_auction_system.unit.modules.social.message.listener;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.dghng36.eauction.core.websocket.enums.SocketEventType;
import com.github.dghng36.eauction.core.websocket.publisher.SocketPublisher;
import com.github.dghng36.eauction.modules.social.message.event.MessageSentEvent;
import com.github.dghng36.eauction.modules.social.message.listener.MessageSocketListener;

@ExtendWith(MockitoExtension.class)
public class MessageSocketListenerTest {
    @Mock private SocketPublisher socketPublisher;

    @InjectMocks private MessageSocketListener messageSocketListener;

    private final String conversationId = "conversation-id-123";
    private final String messageId = "message-id-456";
    private final String senderId = "sender-id-789";
    private MessageSentEvent mockEvent;

    @BeforeEach
    void setUp() {
        mockEvent = MessageSentEvent.builder()
            .messageId(messageId)
            .conversationId(conversationId)
            .senderId(senderId)
            .build();
    }

    @Test
    void handleMessageSent_ValidEvent_ShouldPublishSocket() {
        // Act
        messageSocketListener.handleMessageSent(mockEvent);

        // Assert
        verify(socketPublisher, times(1)).publish(
            eq("/topic/chat/" + conversationId),
            eq(SocketEventType.CHAT_MESSAGE_SENT),
            eq(mockEvent)
        );
    }

    @Test
    void handleMessageSent_NullEvent_ShouldNotPublishSocket() {
        // Act & Assert
        NullPointerException exception = assertThrows(NullPointerException.class, () -> messageSocketListener.handleMessageSent(null));
        assertNotNull(exception);
        
        verify(socketPublisher, never()).publish(any(), any(), any());
    }
}
