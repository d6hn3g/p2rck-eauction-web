package com.github.dghng36.eauction.modules.social.message.listener;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.github.dghng36.eauction.core.websocket.enums.SocketEventType;
import com.github.dghng36.eauction.core.websocket.publisher.SocketPublisher;
import com.github.dghng36.eauction.modules.social.conversation.service.InternalConversationService;
import com.github.dghng36.eauction.modules.social.message.event.MessageDeletedEvent;
import com.github.dghng36.eauction.modules.social.message.event.MessageEditedEvent;
import com.github.dghng36.eauction.modules.social.message.event.MessageReactionEvent;
import com.github.dghng36.eauction.modules.social.message.event.MessageReadEvent;
import com.github.dghng36.eauction.modules.social.message.event.MessageSentEvent;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class MessageSocketListener {
    SocketPublisher socketPublisher;

    InternalConversationService internalConversationService;

    @Async
    @EventListener
    public void handleMessageSent(
        MessageSentEvent event
    ) {

        log.info("New message from conversation: [{}] with message: [{}]", event.getConversationId(), event.getMessageId());

        try {
            internalConversationService.updateLastMessage(
                event.getSenderId(), 
                event.getConversationId(), 
                event.getMessageId(), 
                event.getContent(), 
                event.getCreatedAt()
            );

            internalConversationService.incrementUnreadCount(event.getSenderId(), event.getConversationId());


        } catch (Exception ex) {
            log.error("Failed to update conversation metadata in async flow for message: [{}], error: [{}]:  ", event.getMessageId(), ex.getMessage(), ex);
        }

        try {
            socketPublisher.publish(
                "/topic/chat/" + event.getConversationId(), 
                SocketEventType.CHAT_MESSAGE_SENT, 
                event
            );
        } catch(Exception ex) {
            log.error("Failed to broadcast CHAT_MESSAGE_SENT for conversation: [{}], error: [{}]: ", event.getConversationId(), ex.getMessage(), ex);
        }
    }

    @Async
    @EventListener
    public void handleMessageEdited(
        MessageEditedEvent event
    ) {
        try {
            socketPublisher.publish(
                "/topic/chat/" + event.getConversationId(), 
                SocketEventType.CHAT_MESSAGE_EDITED, 
                event
            );
        } catch (Exception ex) {
            log.error("Failed to broadcast CHAT_MESSAGE_EDITED for conversation: [{}], error: [{}]:  ", event.getConversationId(), ex.getMessage(), ex);
        }
    }

    @Async
    @EventListener
    public void handleMessageRead(
        MessageReadEvent event
    ) {
        try {
            internalConversationService.updateLastMessage(
                event.getReaderId(), 
                event.getConversationId(), 
                event.getLastReadMessageId(), 
                null, null
            );
        } catch (Exception ex) {
            log.error("Failed to update conversation metadata in async flow for message: [{}], error: [{}]: ", event.getLastReadMessageId(), ex.getMessage(), ex);
        }
        
        
        try {
            socketPublisher.publish(
                "/topic/chat/" + event.getConversationId(), 
                SocketEventType.CHAT_MESSAGE_READ, 
                event
            );
        } catch (Exception ex) {
            log.error("Failed to broadcast CHAT_MESSAGE_READ for conversation: [{}], error: [{}]: ", event.getConversationId(), ex.getMessage(), ex);
        }
    }

    @Async
    @EventListener
    public void handleMessageReaction(
        MessageReactionEvent event
    ) {
        try {
            socketPublisher.publish(
                "/topic/chat/" + event.getConversationId(), 
                SocketEventType.CHAT_MESSAGE_REACTION, 
                event
            );
        } catch (Exception ex) {
            log.error("Failed to broadcast CHAT_MESSAGE_REACTION for conversation: [{}], error: [{}]: ", event.getConversationId(), ex.getMessage(), ex);
        }
    }
    
    @Async
    @EventListener
    public void handleMessageDeleted(
        MessageDeletedEvent event
    ) {
        try {
            socketPublisher.publish(
                "/topic/chat/" + event.getConversationId(), 
                SocketEventType.CHAT_MESSAGE_DELETED, 
                event
            );
        } catch (Exception ex) {
            log.error("Failed to broadcast CHAT_MESSAGE_DELETED for conversation: [{}], error: [{}]: ", event.getConversationId(), ex.getMessage(), ex);
        }
    }
}
