package com.github.dghng36.eauction.modules.social.presence.controller.v1;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.dghng36.eauction.core.base.ApiResponse;
import com.github.dghng36.eauction.infra.config.security.annotation.AuthInfo;
import com.github.dghng36.eauction.infra.config.security.annotation.AuthInfoType;
import com.github.dghng36.eauction.modules.social.presence.dto.request.UpdatePresenceRequest;
import com.github.dghng36.eauction.modules.social.presence.dto.response.PresenceResponse;
import com.github.dghng36.eauction.modules.social.presence.service.PresenceService;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;


@RestController
@RequestMapping("/api/v1/presence")
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PresenceController {
    PresenceService presenceService;

    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MANAGER')")
    @GetMapping("/{userId}")
    ResponseEntity<ApiResponse<PresenceResponse>> getUserPresence(
        @PathVariable String userId
    ) {
        PresenceResponse presence = presenceService.getUserPresence(userId);
        return ResponseEntity.ok(ApiResponse.success("User " + userId + " presence retrieved successfully", presence));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MANAGER')")
    @PutMapping("/me")
    ResponseEntity<ApiResponse<PresenceResponse>> updateUserPresence(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @RequestBody UpdatePresenceRequest updatePresenceRequest
    ) {
        log.info("Updating presence for user: [{}] with status: [{}]", userId, updatePresenceRequest.getNewStatus());

        PresenceResponse updatedPresence = presenceService.updateUserPresence(userId, updatePresenceRequest);
        
        return ResponseEntity.ok(ApiResponse.success("User presence updated successfully", updatedPresence));
    }

}
