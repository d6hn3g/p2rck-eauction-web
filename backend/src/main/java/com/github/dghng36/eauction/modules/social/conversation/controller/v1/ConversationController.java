package com.github.dghng36.eauction.modules.social.conversation.controller.v1;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.dghng36.eauction.core.base.ApiResponse;
import com.github.dghng36.eauction.core.base.PageResponse;
import com.github.dghng36.eauction.infra.config.security.annotation.AuthInfo;
import com.github.dghng36.eauction.infra.config.security.annotation.AuthInfoType;
import com.github.dghng36.eauction.modules.social.conversation.dto.request.CreateDirectConversationRequest;
import com.github.dghng36.eauction.modules.social.conversation.dto.request.SearchConversationRequest;
import com.github.dghng36.eauction.modules.social.conversation.dto.response.ConversationDetailResponse;
import com.github.dghng36.eauction.modules.social.conversation.dto.response.ConversationResponse;
import com.github.dghng36.eauction.modules.social.conversation.service.ConversationService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;


@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ConversationController {
    ConversationService conversationService;

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @PostMapping("/direct")
    ResponseEntity<ApiResponse<ConversationResponse>> createDirectConversation(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @Valid @RequestBody CreateDirectConversationRequest createDirectConversationRequest
    ) {
        log.info("Creating direct conversation for user: [{}] with other user: [{}]", userId, createDirectConversationRequest.getRecipientUserId());
        
        ConversationResponse conversationResp = conversationService.createDirectConversation(userId, createDirectConversationRequest);

        
        return ResponseEntity.ok(ApiResponse.success("New direct conversation created", conversationResp));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @GetMapping
    @Validated
    ResponseEntity<ApiResponse<PageResponse<ConversationResponse>>> getMyConversations(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @RequestParam(defaultValue = "0") 
        @Min(value = 0, message = "Page index must not be less than 0")
        int page,

        @RequestParam(defaultValue = "10") 
        @Min(value = 1, message = "Page size must be at least 1")
        @Max(value = 50, message = "Page size must not be greater than 50")
        int size,
        @RequestParam(defaultValue = "lastMessageTime") String sortBy,
        @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        PageResponse<ConversationResponse> conversations = conversationService.getUserConversations(
            userId, page, size, sortBy, sortDirection
        );
        return ResponseEntity.ok(ApiResponse.success("Conversations retrieved successfully", conversations));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @PostMapping("/search")
    @Validated
    ResponseEntity<ApiResponse<PageResponse<ConversationResponse>>> searchConversations(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @Valid @RequestBody SearchConversationRequest searchConversationRequest,
        @RequestParam(defaultValue = "0") 
        @Min(value = 0, message = "Page index must not be less than 0") 
        int page,

        @RequestParam(defaultValue = "10") 
        @Min(value = 1, message = "Page size must be at least 1") 
        @Max(value = 50, message = "Page size must not be greater than 50") 
        int size,
        
        @RequestParam(defaultValue = "lastMessageTime") String sortBy,
        @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        PageResponse<ConversationResponse> conversations = conversationService.searchConversations(
            userId, searchConversationRequest, page, size, sortBy, sortDirection
        );
        return ResponseEntity.ok(ApiResponse.success("Search results retrieved successfully", conversations));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @GetMapping("/{conversationId}")
    ResponseEntity<ApiResponse<ConversationDetailResponse> > getConversationDetail(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @PathVariable String conversationId
    ) {
        ConversationDetailResponse conversationDetail = conversationService.getConversationDetail(userId, conversationId);
        return ResponseEntity.ok(ApiResponse.success("Conversation detail retrieved successfully", conversationDetail));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @PatchMapping("/{conversationId}")
    ResponseEntity<ApiResponse<Void>> hideConversation(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @PathVariable String conversationId
    ) {
        conversationService.hideConversation(userId, conversationId);
        return ResponseEntity.ok(ApiResponse.success("Conversation hidden successfully", null));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @PatchMapping("/{conversationId}/pin")
    ResponseEntity<ApiResponse<Void>> togglePinConversation(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @PathVariable String conversationId,
        @RequestParam Boolean pinned
    ) {
        conversationService.togglePinConversation(userId, conversationId, pinned);
        return ResponseEntity.ok(ApiResponse.success("Conversation pin status toggled successfully", null));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @PatchMapping("/{conversationId}/mute")
    ResponseEntity<ApiResponse<Void>> toggleMuteConversation(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @PathVariable String conversationId,
        @RequestParam Boolean muted
    ) {
        conversationService.toggleMuteConversation(userId, conversationId, muted);
        return ResponseEntity.ok(ApiResponse.success("Conversation mute status toggled successfully", null));
    }

    @PostMapping("/{conversationId}/read")
    ResponseEntity<ApiResponse<Void>> markConversationAsRead(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @PathVariable String conversationId
    ) {
        conversationService.markConversationAsRead(userId, conversationId);
        return ResponseEntity.ok(ApiResponse.success("Conversation marked as read successfully", null));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @PostMapping("/{conversationId}/leave")
    ResponseEntity<ApiResponse<Void>> leaveConversation(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @PathVariable String conversationId
    ) {
        conversationService.leaveConversation(userId, conversationId);
        return ResponseEntity.ok(ApiResponse.success("Left conversation successfully", null));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @DeleteMapping("/{conversationId}")
    ResponseEntity<ApiResponse<Void>> deleteConversation(
        @PathVariable String conversationId
    ) {
        conversationService.deleteConversation(conversationId);
        return ResponseEntity.ok(ApiResponse.success("Conversation deleted successfully", null));
    }
}
