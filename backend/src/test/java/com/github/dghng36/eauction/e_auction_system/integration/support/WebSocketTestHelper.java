package com.github.dghng36.eauction.e_auction_system.integration.support;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class WebSocketTestHelper {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().findAndAddModules().build();

    private WebSocketTestHelper() {}

    public static StompSession connect(String baseUrl, String accessToken) throws Exception {
        WebSocketStompClient stompClient = new WebSocketStompClient(new SockJsClient(
            List.of(new WebSocketTransport(new StandardWebSocketClient()))
        ));

        JacksonJsonMessageConverter modernConverter = new JacksonJsonMessageConverter();

        stompClient.setMessageConverter(new CompositeMessageConverter(List.of(modernConverter)));

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + accessToken);

        CompletableFuture<StompSession> future = new CompletableFuture<>();
        WebSocketHttpHeaders webSocketHttpHeaders = new WebSocketHttpHeaders();

        stompClient.connectAsync(baseUrl + "/ws", webSocketHttpHeaders, connectHeaders, new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                future.complete(session);
            }

            @Override
            public void handleTransportError(StompSession session, Throwable exception) {
                future.completeExceptionally(exception);
            }
        });

        return future.get(15, TimeUnit.SECONDS);
    }

    public static CompletableFuture<Map<String, Object>> subscribe(
        StompSession session,
        String destination
    ) {
        CompletableFuture<Map<String, Object>> messageFuture = new CompletableFuture<>();
        session.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void handleFrame(StompHeaders headers, Object payload) {
                try {
                    Map<String, Object> event = OBJECT_MAPPER.readValue((byte[]) payload, Map.class);
                    messageFuture.complete(event);
                } catch (IOException ex) {
                    messageFuture.completeExceptionally(ex);
                }
            }
        });
        return messageFuture;
    }

    public static void send(StompSession session, String destination, Object payload) {
        session.send(destination, payload);
    }

    public static void disconnect(StompSession session) {
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }

    public static AtomicReference<Map<String, Object>> subscribeCollector(
        StompSession session,
        String destination
    ) {
        AtomicReference<Map<String, Object>> latest = new AtomicReference<>();
        session.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void handleFrame(StompHeaders headers, Object payload) {
                try {
                    byte[] data = (byte[]) payload;
                    // In thô chuỗi JSON nhận được để kiểm tra nếu bài test bị treo (Timeout)
                    log.info("WebSocket received payload from {}: {}", destination, new String(data));

                    latest.set(OBJECT_MAPPER.readValue(data, Map.class));
                } catch (IOException ex) {
                    // Sửa lỗi đa catch hợp lệ theo IDE, ghi nhận log lỗi rõ ràng
                    log.error("Lỗi parse dữ liệu JSON trên kênh WebSocket tại đường dẫn {}: {}", destination, ex.getMessage());
                }
            }
        });
        return latest;
    }
}
