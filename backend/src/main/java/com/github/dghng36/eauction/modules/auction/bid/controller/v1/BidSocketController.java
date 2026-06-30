package com.github.dghng36.eauction.modules.auction.bid.controller.v1;

import java.security.Principal;

import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import com.github.dghng36.eauction.core.base.ApiResponse;
import com.github.dghng36.eauction.core.base.AuthInfoDto;
import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.infra.config.security.annotation.AuthInfo;
import com.github.dghng36.eauction.infra.config.security.annotation.AuthInfoType;
import com.github.dghng36.eauction.modules.auction.bid.dto.request.DisableAutoBidSocketRequest;
import com.github.dghng36.eauction.modules.auction.bid.dto.request.EnableAutoBidSocketRequest;
import com.github.dghng36.eauction.modules.auction.bid.dto.request.PlaceBidSocketRequest;
import com.github.dghng36.eauction.modules.auction.bid.service.BidSocketService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class BidSocketController {
    SimpMessagingTemplate messagingTemplate;
    BidSocketService bidSocketService;

    @MessageMapping("/auction.bid.place")
    public void placeBid(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @Valid PlaceBidSocketRequest placeBidSocketRequest
    ) {
        log.info("New bid placed from user: [{}] for auction room: [{}]", userId, placeBidSocketRequest.getAuctionRoomId());

        bidSocketService.placeBid(userId, placeBidSocketRequest);
    }

    @MessageMapping("/auction.bid.auto.enable")
    public void enableAutoBid(
        @AuthInfo(info = AuthInfoType.ID) String userId, 
        @Valid EnableAutoBidSocketRequest enableAutoBidSocketRequest
    ) {
        log.info("Auto bid enabled from user: [{}] for auction room: [{}]", userId, enableAutoBidSocketRequest.getAuctionRoomId());

        bidSocketService.enableAutoBid(userId, enableAutoBidSocketRequest);
    }

    @MessageMapping("/auction.bid.auto.disable")
    public void disableAutoBid(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @Valid DisableAutoBidSocketRequest disableAutoBidSocketRequest
    ) {
        log.info("Auto bid disabled from user: [{}] for auction room: [{}]", userId, disableAutoBidSocketRequest.getAuctionRoomId());

        bidSocketService.disableAutoBid(userId, disableAutoBidSocketRequest);
    }

    // Exception socket handler
    @MessageExceptionHandler(AppException.class)
    public void handleWebSocketAppException(AppException ex, Principal principal) {
        String userId = "N/A";

        if (principal instanceof Authentication authentication) {
            Object principalDetails = authentication.getPrincipal();
            
            if (principalDetails instanceof AuthInfoDto authInfo) {
                userId = authInfo.getId();
            } else if (principalDetails != null) {
                userId = principalDetails.toString();
            }
        } else if (principal != null) {
            userId = principal.getName();
        }

        log.warn("WebSocket business error for user [{}]: [{}]", userId, ex.getMessage(), ex);

        if (!"N/A".equals(userId)) {
            messagingTemplate.convertAndSendToUser(
                userId, 
                "/queue/errors", 
                ApiResponse.error(ex.getMessage(), ex.getStatus().value())
            );
        }
    }

    @MessageExceptionHandler(Exception.class)
    public void handleWebSocketGenericException(Exception ex, Principal principal) {
        log.error("Critical WebSocket Infrastructure error: ", ex);

        if (principal != null) {
            messagingTemplate.convertAndSendToUser(
                principal.getName(), 
                "/queue/errors", 
                ApiResponse.error("An unexpected error occurred during bidding. Please try again.", HttpStatus.INTERNAL_SERVER_ERROR.value())
            );
        }
    }
}
