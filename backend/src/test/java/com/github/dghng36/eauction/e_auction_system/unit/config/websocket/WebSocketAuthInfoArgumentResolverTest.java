package com.github.dghng36.eauction.e_auction_system.unit.config.websocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.github.dghng36.eauction.core.base.AuthInfoDto;
import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.infra.config.security.annotation.AuthInfo;
import com.github.dghng36.eauction.infra.config.security.annotation.AuthInfoType;
import com.github.dghng36.eauction.infra.config.websocket.resolver.WebSocketAuthInfoArgumentResolver;
import com.github.dghng36.eauction.modules.identity.enums.UserRole;

@ExtendWith(MockitoExtension.class)
public class WebSocketAuthInfoArgumentResolverTest {
    @InjectMocks
    private WebSocketAuthInfoArgumentResolver webSocketAuthInfoArgumentResolver;


    @Mock
    private MethodParameter methodParameter;

    @Mock
    private AuthInfo authInfoAnnotation;

    private Message<?> createMessageWithUser(AuthInfoDto dto) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
        if (dto != null) {
            accessor.setUser(new UsernamePasswordAuthenticationToken(dto, null));
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    /**
     * Test cases for WebSocketAuthInfoArgumentResolver
     * Tests:
     * - resolveArgument_WithIdType_ShouldReturnUserId
     * - resolveArgument_WithUnauthenticatedSession_ShouldThrowUnauthorized
     */

    @Test
    void resolveArgument_WithIdType_ShouldReturnUserId() throws Exception {
        // Arrange
        AuthInfoDto mockDto = AuthInfoDto.builder()
            .id("testUserDto")
            .username("testUserDto")
            .role(UserRole.USER)
            .build();


        Message<?> message = createMessageWithUser(mockDto);

        when(methodParameter.getParameterAnnotation(AuthInfo.class)).thenReturn(authInfoAnnotation);
        when(authInfoAnnotation.info()).thenReturn(AuthInfoType.ID);

        // Act
        Object result = webSocketAuthInfoArgumentResolver.resolveArgument(methodParameter, message);

        // Assert
        assertEquals("testUserDto", result);
    }

    @Test
    void resolveArgument_WithUnauthenticatedSession_ShouldThrowUnauthorized() {
        Message<?> message = createMessageWithUser(null);

        when(methodParameter.getParameterAnnotation(AuthInfo.class)).thenReturn(authInfoAnnotation);

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> 
            webSocketAuthInfoArgumentResolver.resolveArgument(methodParameter, message)
        );
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertEquals("Unauthenticated websocket session", exception.getMessage());
    }

}
