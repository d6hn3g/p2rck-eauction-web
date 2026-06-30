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
import com.github.dghng36.eauction.infra.config.security.annotation.AuthInfo;
import com.github.dghng36.eauction.infra.config.security.annotation.AuthInfoType;
import com.github.dghng36.eauction.modules.auction.bid.dto.request.SearchUserBidsRequest;
import com.github.dghng36.eauction.modules.auction.bid.dto.response.BidResponse;
import com.github.dghng36.eauction.modules.auction.bid.service.BidService;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;


@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class UserBidController {
    BidService bidService;

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @GetMapping("/{userId}/auctions/rooms/bids")
    @Validated
    ResponseEntity<ApiResponse<PageResponse<BidResponse>>> getUserBidHistories(
        @PathVariable String userId,
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
        PageResponse<BidResponse> bidHistories = bidService.getUserBidHistories(
            userId, 
            page, size, 
            sortBy, sortDirection
        );

        return ResponseEntity
            .ok(ApiResponse.success("Get user bid histories successfully", bidHistories));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @PostMapping("/{userId}/auctions/rooms/bids/search")
    @Validated
    ResponseEntity<ApiResponse<PageResponse<BidResponse>>> searchUserBidHistories(
        @PathVariable String userId,
        @RequestParam(defaultValue = "0") 
        @Min(value = 0, message = "Page index must not be less than 0")
        int page,

        @RequestParam(defaultValue = "10") 
        @Min(value = 1, message = "Page size must not be less than 1")
        @Max(value = 50, message = "Page size must not be greater than 50")
        int size,
        
        @RequestParam(required = false, defaultValue = "bidTime") String sortBy,
        @RequestParam(required = false, defaultValue = "desc") String sortDirection,
        @RequestBody SearchUserBidsRequest searchUserBidHistoriesRequest
    ) {
        PageResponse<BidResponse> bidHistories = bidService.searchUserBidHistories(
            userId, searchUserBidHistoriesRequest,
            page, size, 
            sortBy, sortDirection
        );

        return ResponseEntity
            .ok(ApiResponse.success("Search user bid histories successfully", bidHistories));
    }

    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @GetMapping("/me/auctions/rooms/bids")
    @Validated
    ResponseEntity<ApiResponse<PageResponse<BidResponse>>> getMyBidHistories(
        @AuthInfo(info = AuthInfoType.ID) String userId,
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
        PageResponse<BidResponse> bidHistories = bidService.getUserBidHistories(
            userId,
            page, size, 
            sortBy, sortDirection
        );

        return ResponseEntity
            .ok(ApiResponse.success("Get my bid histories successfully", bidHistories));
    }

    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @PostMapping("/me/auctions/rooms/bids/search")
    @Validated
    ResponseEntity<ApiResponse<PageResponse<BidResponse>>> searchMyBidHistories(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @RequestParam(defaultValue = "0") 
        @Min(value = 0, message = "Page index must not be less than 0")
        int page,

        @RequestParam(defaultValue = "10") 
        @Min(value = 1, message = "Page size must not be less than 1")
        @Max(value = 50, message = "Page size must not be greater than 50")
        int size,

        @RequestParam(required = false, defaultValue = "bidTime") String sortBy,
        @RequestParam(required = false, defaultValue = "desc") String sortDirection,
        @RequestBody SearchUserBidsRequest searchUserBidsRequest
    ) {
        PageResponse<BidResponse> bidHistories = bidService.searchUserBidHistories(
            userId, searchUserBidsRequest,
            page, size, 
            sortBy, sortDirection
        );

        return ResponseEntity
            .ok(ApiResponse.success("Search my bid histories successfully", bidHistories));
    }
}
