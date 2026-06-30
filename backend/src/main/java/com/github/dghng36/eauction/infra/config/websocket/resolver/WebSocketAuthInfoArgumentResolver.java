package com.github.dghng36.eauction.infra.config.websocket.resolver;

import java.security.Principal;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.github.dghng36.eauction.core.base.AuthInfoDto;
import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.infra.config.security.annotation.AuthInfo;

@Component
public class WebSocketAuthInfoArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(
        MethodParameter parameter
    ) {
        return parameter.hasParameterAnnotation(AuthInfo.class);
    }

    @Override
    public Object resolveArgument(
        MethodParameter parameter,
        Message<?> message
    ) {
        AuthInfo authInfoAnnotation =
            parameter.getParameterAnnotation(AuthInfo.class);

        if (authInfoAnnotation == null) {
            return null;
        }

        Principal principal =
            SimpMessageHeaderAccessor
                .wrap(message)
                .getUser();

        if (!(principal instanceof Authentication authentication)) {
            throw new AppException(
                "Unauthenticated websocket session",
                HttpStatus.UNAUTHORIZED
            );
        }

        AuthInfoDto authInfo =
            (AuthInfoDto) authentication.getPrincipal();

        return switch (authInfoAnnotation.info()) {

            case ID ->
                authInfo.getId();

            case ROLE ->
                authInfo.getRole();

            case USERNAME ->
                authInfo.getUsername();
                
            case DEFAULT ->
                authInfo;

            default -> 
                authInfo;
        };
    }
}
