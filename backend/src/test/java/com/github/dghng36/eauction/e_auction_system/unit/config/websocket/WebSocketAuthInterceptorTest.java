package com.github.dghng36.eauction.e_auction_system.unit.config.websocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.Authentication;

import com.github.dghng36.eauction.core.base.AuthInfoDto;
import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.infra.config.websocket.interceptor.WebSocketAuthInterceptor;
import com.github.dghng36.eauction.modules.identity.auth.service.jwt.JwtAdapter;
import com.github.dghng36.eauction.modules.identity.enums.UserRole;

@ExtendWith(MockitoExtension.class)
public class WebSocketAuthInterceptorTest {
    @Mock
    private JwtAdapter jwtAdapter;

    @Mock
    private MessageChannel messageChannel;

    @InjectMocks
    private WebSocketAuthInterceptor webSocketAuthInterceptor;

    private final String validToken = "valid.jwt.token";

    /**
     * Test cases for WebSocketAuthInterceptor
     * Tests:
     * - preSend_WithValidToken_ShouldSetUserInAccessor
     * - preSend_MissingToken_ShouldThrowUnauthorized
     * - preSend_InvalidToken_ShouldThrowUnauthorized
     */

    @Test
    public void preSend_WithValidToken_ShouldSetUserInAccessor() {
        // Arrange
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);

        accessor.setNativeHeader("Authorization", "Bearer " + validToken);

        accessor.setLeaveMutable(true);

        Message<byte[]> message = MessageBuilder.createMessage(
            new byte[0],
            accessor.getMessageHeaders()
        );

        when(jwtAdapter.validateAccessToken(validToken)).thenReturn(true);
        when(jwtAdapter.extractData(validToken, "userId")).thenReturn("user-123");
        when(jwtAdapter.extractData(validToken, "username")).thenReturn("testSocketUser");
        when(jwtAdapter.extractData(validToken, "role")).thenReturn(UserRole.USER.name());

        // Act
        Message<?> resultMessage = webSocketAuthInterceptor.preSend(message, messageChannel);

        // Assert
        assertNotNull(resultMessage);

        Authentication auth = (Authentication) accessor.getUser();

        assertNotNull(auth);
        assertTrue(auth.isAuthenticated());

        AuthInfoDto principal = (AuthInfoDto) auth.getPrincipal();

        assertEquals("user-123", principal.getId());
        assertEquals("testSocketUser", principal.getUsername());
        assertEquals(UserRole.USER, principal.getRole());
    }

    @Test
    public void preSend_InvalidToken_ShouldThrowUnauthorized() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer invalid-token");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtAdapter.validateAccessToken("invalid-token")).thenReturn(false);

        // Act & Assert
        AppException exception = assertThrows(
            AppException.class, () -> webSocketAuthInterceptor.preSend(message, messageChannel)
        );
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    @Test
    void preSend_NotConnectCommand_ShouldIgnoreInterceptor() {
        // Arrange
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // Act
        Message<?> result = webSocketAuthInterceptor.preSend(message, messageChannel);

        // Assert
        assertSame(message, result);
        verifyNoInteractions(jwtAdapter);
    }

}
