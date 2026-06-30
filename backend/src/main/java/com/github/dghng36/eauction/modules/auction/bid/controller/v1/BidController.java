package com.github.dghng36.eauction.modules.auction.bid.controller.v1;


import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.dghng36.eauction.core.base.ApiResponse;
import com.github.dghng36.eauction.core.base.PageResponse;
import com.github.dghng36.eauction.modules.auction.bid.dto.request.SearchBidsRequest;
import com.github.dghng36.eauction.modules.auction.bid.dto.response.BidResponse;
import com.github.dghng36.eauction.modules.auction.bid.service.BidService;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;


@RestController
@RequestMapping("/api/v1/auctions")
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class BidController {
    BidService bidService;

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @GetMapping("/rooms/{roomId}/bids")
    @Validated
    ResponseEntity<ApiResponse<PageResponse<BidResponse>>> getAuctionRoomBidHistories(
        @PathVariable String roomId,
        @RequestParam(defaultValue = "0") 
        @Min(value = 0, message = "Page index must not be less than 0")
        int page,

        @RequestParam(defaultValue = "10") 
        @Min(value = 1, message = "Page size must not be less than 1")
        @Max(value = 50, message = "Page size must not be greater than 50")
        int size,

        @RequestParam(required = false, defaultValue = "bidTime") String sortBy,
        @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        PageResponse<BidResponse> bidHistories = bidService.getAuctionRoomBidHistories(
            roomId, 
            page, size,
            sortBy, sortDirection
        );

        return ResponseEntity
            .ok(ApiResponse.success("Get auction room bid histories successfully", bidHistories));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @PostMapping("/rooms/{roomId}/bids/search")
    ResponseEntity<ApiResponse<PageResponse<BidResponse>>> searchAuctionRoomBidHistories(
        @PathVariable String roomId,
        @RequestParam(required = false, defaultValue = "0") 
        @Min(value = 0, message = "Page index must not be less than 0")
        int page,

        @RequestParam(required = false, defaultValue = "10") 
        @Min(value = 1, message = "Page size must not be less than 1")
        @Max(value = 50, message = "Page size must not be greater than 50")
        int size,
        
        @RequestParam(required = false, defaultValue = "bidTime") String sortBy,
        @RequestParam(required = false, defaultValue = "desc") String sortDirection,
        @RequestBody SearchBidsRequest searchAuctionRoomBidHistoriesRequest
    ) {
        PageResponse<BidResponse> bidHistories = bidService.searchAuctionRoomBidHistories(
            roomId,
            page, size,
            sortBy, sortDirection,
            searchAuctionRoomBidHistoriesRequest
        );

        return ResponseEntity
            .ok(ApiResponse.success("Search auction room bid histories successfully", bidHistories));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @GetMapping("/bids/{bidId}")
    ResponseEntity<ApiResponse<BidResponse>> getBidDetail(
        @PathVariable String bidId
    ) {
        BidResponse bid = bidService.getBidDetail(bidId);

        return ResponseEntity
            .ok(ApiResponse.success("Get bid detail by id successfully", bid));
    }
    
}
