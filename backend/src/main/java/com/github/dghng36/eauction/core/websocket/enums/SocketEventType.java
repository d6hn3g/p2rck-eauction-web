package com.github.dghng36.eauction.core.websocket.enums;

/**
 * Websocket event types for transmitting auction-related events
 * It goes coming from backend straight to frontend, 
 * so it should be as specific as possible to avoid confusion and make it easier to handle in frontend    
 **/

public enum SocketEventType {
    EXCEPTION,

    NEW_BID,
    BID_OUTBID,

    CHAT_MESSAGE_SENT,
    CHAT_MESSAGE_EDITED,
    CHAT_MESSAGE_READ,
    CHAT_MESSAGE_REACTION,
    CHAT_MESSAGE_DELETED,

    CHAT_MESSAGE_TYPING,

    NOTIFICATION_CREATED,
    NOTIFICATION_READ;
}

