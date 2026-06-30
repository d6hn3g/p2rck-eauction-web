package com.github.dghng36.eauction.infra.config.websocket.interceptor;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.github.dghng36.eauction.core.base.AuthInfoDto;
import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.modules.identity.auth.service.jwt.JwtAdapter;
import com.github.dghng36.eauction.modules.identity.enums.UserRole;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class WebSocketAuthInterceptor implements ChannelInterceptor {
    JwtAdapter jwtAdapter;

    @Override
    public Message<?> preSend(
        Message<?> message,
        MessageChannel channel
    ) {
        StompHeaderAccessor accessor =
            MessageHeaderAccessor.getAccessor(
                message,
                StompHeaderAccessor.class
            );

        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {

            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (!StringUtils.hasText(authHeader)
                || !authHeader.startsWith("Bearer ")
            ) {

                throw new AppException("Missing authorization token", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            if (!jwtAdapter.validateAccessToken(token)) {
                throw new AppException("Invalid or expired authorization token", HttpStatus.UNAUTHORIZED);
            }

            String userId = jwtAdapter.extractData(token, "userId");
            String username = jwtAdapter.extractData(token, "username");
            UserRole role = UserRole.fromString(
                jwtAdapter.extractData(token, "role")
                    .replaceFirst("^ROLE_", "")
            ).orElse(null);

            AuthInfoDto authInfoDto = AuthInfoDto.builder()
                .id(userId)
                .username(username)
                .role(role)
                .build();

            String springSecurityRole = (role != null) ? "ROLE_" + role.name() : "ROLE_USER";

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                authInfoDto,
                null,
                List.of(new SimpleGrantedAuthority(springSecurityRole))
            );

            accessor.setUser(authentication);
        }

        return message;
    }
}
