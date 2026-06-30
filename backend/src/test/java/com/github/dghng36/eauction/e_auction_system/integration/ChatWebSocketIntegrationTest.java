package com.github.dghng36.eauction.e_auction_system.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.messaging.simp.stomp.StompSession;

import com.github.dghng36.eauction.core.base.ApiResponse;
import com.github.dghng36.eauction.e_auction_system.integration.support.AbstractIntegrationTest;
import com.github.dghng36.eauction.e_auction_system.integration.support.TestUserSession;
import com.github.dghng36.eauction.e_auction_system.integration.support.WebSocketTestHelper;
import com.github.dghng36.eauction.modules.social.chat.dto.internal.ChatSocketMessage;
import com.github.dghng36.eauction.modules.social.conversation.dto.request.CreateDirectConversationRequest;
import com.github.dghng36.eauction.modules.social.conversation.dto.response.ConversationResponse;
import com.github.dghng36.eauction.modules.social.enums.MessageType;

class ChatWebSocketIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    private TestUserSession sender;
    private TestUserSession receiver;

    @BeforeEach
    void setUpUsers() {
        mongoTemplate.getDb().drop();
        sender = registerAndLogin("chat_sender");
        receiver = registerAndLogin("chat_receiver");
    }

    @Test
    void chatWebSocket_SendMessage_ShouldBroadcastChatMessageSentEvent() throws Exception {
        ApiResponse<ConversationResponse> conversationResponse = postAuthenticated(
            "/api/v1/conversations/direct",
            CreateDirectConversationRequest.builder()
                .recipientUserId(receiver.getUserId())
                .build(),
            sender.getAccessToken(),
            new ParameterizedTypeReference<>() {}
        );
        String conversationId = conversationResponse.getData().getId();

        String baseUrl = "http://localhost:" + port;
        StompSession receiverSession = WebSocketTestHelper.connect(baseUrl, receiver.getAccessToken());
        AtomicReference<Map<String, Object>> chatEventRef = WebSocketTestHelper.subscribeCollector(
            receiverSession,
            "/topic/chat/" + conversationId
        );

        StompSession senderSession = WebSocketTestHelper.connect(baseUrl, sender.getAccessToken());
        WebSocketTestHelper.send(
            senderSession,
            "/app/chat.send",
            ChatSocketMessage.builder()
                .conversationId(conversationId)
                .content("Hello from integration test")
                .type(MessageType.TEXT)
                .build()
        );

        await().atMost(15, TimeUnit.SECONDS).until(() -> {
            Map<String, Object> event = chatEventRef.get();
            return event != null && "CHAT_MESSAGE_SENT".equals(event.get("eventType"));
        });

        assertThat(chatEventRef.get().get("eventType")).isEqualTo("CHAT_MESSAGE_SENT");

        WebSocketTestHelper.disconnect(senderSession);
        WebSocketTestHelper.disconnect(receiverSession);
    }
}
