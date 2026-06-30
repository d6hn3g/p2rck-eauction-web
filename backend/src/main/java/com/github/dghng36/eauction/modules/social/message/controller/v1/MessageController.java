package com.github.dghng36.eauction.modules.social.message.controller.v1;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.dghng36.eauction.core.base.ApiResponse;
import com.github.dghng36.eauction.core.base.PageResponse;
import com.github.dghng36.eauction.infra.config.security.annotation.AuthInfo;
import com.github.dghng36.eauction.infra.config.security.annotation.AuthInfoType;
import com.github.dghng36.eauction.modules.social.message.dto.request.AddReactionMessageRequest;
import com.github.dghng36.eauction.modules.social.message.dto.request.EditMessageRequest;
import com.github.dghng36.eauction.modules.social.message.dto.request.SearchMessagesRequest;
import com.github.dghng36.eauction.modules.social.message.dto.response.MessageResponse;
import com.github.dghng36.eauction.modules.social.message.service.MessageService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;


@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class MessageController {
    MessageService messageService;

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @GetMapping("/conversations/{conversationId}")
    @Validated
    ResponseEntity<ApiResponse<PageResponse<MessageResponse>>> getMessages(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @PathVariable String conversationId,
        @RequestParam(defaultValue = "0") 
        @Min(value = 0, message = "Page index must not be less than 0")
        int page,

        @RequestParam(defaultValue = "20") 
        @Min(value = 1, message = "Page size must be at least 1")
        @Max(value = 50, message = "Page size must not be greater than 50")
        int size
    ) {
        PageResponse<MessageResponse> messagePage = messageService.getMessages(
            userId,
            conversationId,
            page, size
        );

        return ResponseEntity.ok(ApiResponse.success("Messages retrieved successfully", messagePage));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @PostMapping("/conversations/{conversationId}/search")
    @Validated
    ResponseEntity<ApiResponse<PageResponse<MessageResponse>>> searchMessages(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @PathVariable String conversationId,
        @Valid @RequestBody SearchMessagesRequest searchMessagesRequest,
        @RequestParam(defaultValue = "0") 
        @Min(value = 0, message = "Page index must not be less than 0")
        int page,

        @RequestParam(defaultValue = "20") 
        @Min(value = 1, message = "Page size must be at least 1")
        @Max(value = 50, message = "Page size must not be greater than 50")
        int size
    ) {
        PageResponse<MessageResponse> messagePage = messageService.searchMessages(
            userId,
            conversationId,
            searchMessagesRequest,
            page, size
        );
        
        return ResponseEntity.ok(ApiResponse.success("Messages retrieved successfully", messagePage));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @GetMapping("/{messageId}")
    ResponseEntity<ApiResponse<MessageResponse>> getMessageDetail(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @PathVariable String messageId
    ) {
        MessageResponse message = messageService.getMessageDetail(userId, messageId);

        return ResponseEntity.ok(ApiResponse.success("Message retrieved successfully", message));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @PutMapping("/{messageId}/edit")
    ResponseEntity<ApiResponse<MessageResponse>> editMessage(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @PathVariable String messageId,
        @Valid @RequestBody EditMessageRequest editMessageRequest
    ) {
        MessageResponse editedMessage = messageService.editMessage(userId, messageId, editMessageRequest);

        return ResponseEntity.ok(ApiResponse.success("Message edited successfully", editedMessage));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @DeleteMapping("/{messageId}")
    ResponseEntity<ApiResponse<Void>> deleteMessage(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @PathVariable String messageId
    ) {
        messageService.deleteMessageForMe(userId, messageId);

        return ResponseEntity.ok(ApiResponse.success("Message deleted successfully", null));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @PostMapping("/{messageId}/unsend")
    ResponseEntity<ApiResponse<Void>> unsendMessage(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @PathVariable String messageId
    ) {
        messageService.deleteMessageForEveryone(userId, messageId);

        return ResponseEntity.ok(ApiResponse.success("Message unsent successfully", null));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @PostMapping("/{messageId}/reactions")
    ResponseEntity<ApiResponse<Void>> addReaction(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @PathVariable String messageId,
        @RequestBody AddReactionMessageRequest addReactionMessageRequest
    ) {
        messageService.addReaction(userId, messageId, addReactionMessageRequest);

        return ResponseEntity.ok(ApiResponse.success("Reaction added successfully", null));

    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @DeleteMapping("/{messageId}/reactions")
    ResponseEntity<ApiResponse<Void>> removeReaction(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @PathVariable String messageId,
        @RequestParam String emoji
    ) {
        messageService.removeReaction(userId, messageId, emoji);

        return ResponseEntity.ok(ApiResponse.success("Reaction removed successfully", null));
    }
    
}
