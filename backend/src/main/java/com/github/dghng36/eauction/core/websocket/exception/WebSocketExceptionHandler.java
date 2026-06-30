package com.github.dghng36.eauction.core.websocket.exception;

import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.stereotype.Controller;

import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.core.websocket.publisher.SocketPublisher;
import com.github.dghng36.eauction.infra.config.security.annotation.AuthInfo;
import com.github.dghng36.eauction.infra.config.security.annotation.AuthInfoType;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Controller
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class WebSocketExceptionHandler {
    SocketPublisher socketPublisher;

    @MessageExceptionHandler(AppException.class)
    public void handleAppException(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        AppException ex
    ) {
        socketPublisher.publishException(userId, ex.getMessage());
    }
}
