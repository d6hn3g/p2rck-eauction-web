package com.github.dghng36.eauction.modules.social.presence.mapper;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.modules.social.enums.PresenceStatus;
import com.github.dghng36.eauction.modules.social.presence.dto.response.PresenceResponse;
import com.github.dghng36.eauction.modules.social.presence.model.Presence;

@Component
public class PresenceMapper {
    public Presence toPresenceEntity(
        String userId,
        String status
    ) {
        PresenceStatus presenceStatus = PresenceStatus.fromString(status)
            .orElseThrow(() -> new AppException("Invalid presence status: " + status, HttpStatus.BAD_REQUEST));

        return Presence.builder()
            .userId(userId)
            .status(presenceStatus)
            .lastSeen(Instant.now())
            .build();
    }

    public PresenceResponse toPresenceResponse(Presence presence) {
        return PresenceResponse.builder()
            .userId(presence.getUserId())
            .status(presence.getStatus().name())
            .lastSeen(presence.getLastSeen())
            .build();
    }
}
