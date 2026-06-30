package com.github.dghng36.eauction.core.websocket.event;

import java.time.Instant;

import com.github.dghng36.eauction.core.websocket.enums.SocketEventType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class SocketEvent<T> {
    SocketEventType eventType;
    
    T data;

    Instant timestamp;
}
