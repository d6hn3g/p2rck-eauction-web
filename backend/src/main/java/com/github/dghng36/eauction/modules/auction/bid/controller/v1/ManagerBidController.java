package com.github.dghng36.eauction.modules.auction.bid.controller.v1;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.dghng36.eauction.core.base.ApiResponse;
import com.github.dghng36.eauction.infra.config.security.annotation.AuthInfo;
import com.github.dghng36.eauction.infra.config.security.annotation.AuthInfoType;
import com.github.dghng36.eauction.modules.auction.bid.service.BidService;
import com.github.dghng36.eauction.modules.identity.enums.UserRole;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/manager/management/auctions/")
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ManagerBidController {
    BidService bidService;

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping("/rooms/{roomId}/bids/total")
    ResponseEntity<ApiResponse<Long>> getTotalAuctionRoomBidHistories(
        @AuthInfo(info = AuthInfoType.ROLE) UserRole userRole,
        @PathVariable String roomId
    ) {
        long totalBids = bidService.getTotalAuctionRoomBidHistories(roomId);
        return ResponseEntity
            .ok(ApiResponse.success("Get total auction room bid histories successfully", totalBids));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @DeleteMapping("/rooms/{roomId}/bids/{bidHistoryId}")
    ResponseEntity<ApiResponse<Void>> deleteAuctionRoomBidHistory(
        @PathVariable String roomId,
        @PathVariable String bidHistoryId
    ) {
        log.info("Deleting bid history with bid: [{}] for auction room: [{}]", bidHistoryId, roomId);

        bidService.deleteAuctionRoomBidHistory(roomId, bidHistoryId);

        return ResponseEntity
            .ok(ApiResponse.success("Delete auction room bid history successfully"));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @DeleteMapping("/rooms/{roomId}/bids")
    ResponseEntity<ApiResponse<Void>> deleteAllAuctionRoomBidHistories(
        @PathVariable String roomId
    ) {
        log.info("Deleting all bid histories for auction room: [{}]", roomId);

        bidService.deleteAllAuctionRoomBidHistories(roomId);

        return ResponseEntity
            .ok(ApiResponse.success("Delete all auction room bid histories successfully"));
    }
}

