package com.github.dghng36.eauction.e_auction_system.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.simp.stomp.StompSession;

import com.github.dghng36.eauction.e_auction_system.integration.support.AbstractIntegrationTest;
import com.github.dghng36.eauction.e_auction_system.integration.support.TestUserSession;
import com.github.dghng36.eauction.e_auction_system.integration.support.WebSocketTestHelper;

class NotificationWebSocketIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    private TestUserSession user;

    @BeforeEach
    void setUpUser() {
        mongoTemplate.getDb().drop();
        user = registerAndLogin("notif_user");
    }

    @Test
    void notificationWebSocket_SubscribeToUserTopic_ShouldConnectSuccessfully() throws Exception {
        String baseUrl = "http://localhost:" + port;
        StompSession session = WebSocketTestHelper.connect(baseUrl, user.getAccessToken());
        AtomicReference<Map<String, Object>> notificationRef = WebSocketTestHelper.subscribeCollector(
            session,
            "/topic/notifications/" + user.getUserId()
        );

        assertThat(session.isConnected()).isTrue();

        await().during(2, TimeUnit.SECONDS).until(() -> notificationRef.get() == null);

        WebSocketTestHelper.disconnect(session);
    }
}
