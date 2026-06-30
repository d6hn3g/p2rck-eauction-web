package com.github.dghng36.eauction.modules.auction.auctionRoom.controller.v1;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.github.dghng36.eauction.core.base.ApiResponse;
import com.github.dghng36.eauction.core.base.PageResponse;
import com.github.dghng36.eauction.infra.config.security.annotation.AuthInfo;
import com.github.dghng36.eauction.infra.config.security.annotation.AuthInfoType;
import com.github.dghng36.eauction.modules.auction.auctionRoom.dto.request.CancelAuctionRoomRequest;
import com.github.dghng36.eauction.modules.auction.auctionRoom.dto.request.CreateAuctionRoomRequest;
import com.github.dghng36.eauction.modules.auction.auctionRoom.dto.request.SearchAuctionRoomsRequest;
import com.github.dghng36.eauction.modules.auction.auctionRoom.dto.request.UpdateAuctionRoomRequest;
import com.github.dghng36.eauction.modules.auction.auctionRoom.dto.response.AuctionRoomResponse;
import com.github.dghng36.eauction.modules.auction.auctionRoom.service.AuctionRoomService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserAuctionRoomController {
    AuctionRoomService auctionRoomService;

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @GetMapping("/{userId}/auctions/rooms")
    @Validated
    ResponseEntity<ApiResponse<PageResponse<AuctionRoomResponse>>> getPublicUserAuctionRooms(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @RequestParam(defaultValue = "0") 
        @Min(value = 0, message = "Page index must not be less than 0")
        int page,
        
        @RequestParam(defaultValue = "10") 
        @Min(value = 1, message = "Page size must not be less than 1")
        @Max(value = 50, message = "Page size must not be greater than 50")
        int size,

        @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
        @RequestParam(required = false, defaultValue = "asc") String sortDirection
    ) {
        PageResponse<AuctionRoomResponse> myAuctionRooms = auctionRoomService.getUserAuctionRooms(
            userId, 
            page, size, 
            sortBy, sortDirection
        );

        return ResponseEntity
            .ok(ApiResponse.success("Get my auction rooms successfully", myAuctionRooms));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @GetMapping("/{userId}/auctions/rooms/created")
    @Validated
    ResponseEntity<ApiResponse<PageResponse<AuctionRoomResponse>>> getPublicUserCreatedAuctionRooms(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @RequestParam(defaultValue = "0") 
        @Min(value = 0, message = "Page index must not be less than 0")
        int page,

        @RequestParam(defaultValue = "10") 
        @Min(value = 1, message = "Page size must not be less than 1")
        @Max(value = 50, message = "Page size must not be greater than 50")
        int size,

        @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
        @RequestParam(required = false, defaultValue = "asc") String sortDirection
    ) {
        PageResponse<AuctionRoomResponse> myCreatedAuctionRooms = auctionRoomService.getOwnerCreatedAuctionRooms(
            userId, 
            page, size, 
            sortBy, sortDirection
        );

        return ResponseEntity
            .ok(ApiResponse.success("Get my created auction rooms successfully", myCreatedAuctionRooms));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @GetMapping("/{userId}/auctions/rooms/joined")
    @Validated
    ResponseEntity<ApiResponse<PageResponse<AuctionRoomResponse>>> getPublicUserJoinedAuctionRooms(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @RequestParam(defaultValue = "0") 
        @Min(value = 0, message = "Page index must not be less than 0")
        int page,

        @RequestParam(defaultValue = "10") 
        @Min(value = 1, message = "Page size must not be less than 1")
        @Max(value = 50, message = "Page size must not be greater than 50")
        int size,

        @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
        @RequestParam(required = false, defaultValue = "asc") String sortDirection
    ) {
        PageResponse<AuctionRoomResponse> myJoinedAuctionRooms = auctionRoomService.getUserJoinedAuctionRooms(
            userId, 
            page, size, 
            sortBy, sortDirection
        );

        return ResponseEntity
            .ok(ApiResponse.success("Get my joined auction rooms successfully", myJoinedAuctionRooms));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @PostMapping("/me/auctions/rooms")
    ResponseEntity<ApiResponse<AuctionRoomResponse>> createMyAuctionRoom(@AuthInfo(info = AuthInfoType.ID) String userId, @Valid @RequestBody CreateAuctionRoomRequest createAuctionRoomRequest) {
        AuctionRoomResponse auctionRoom = auctionRoomService.createMyAuctionRoom(userId, createAuctionRoomRequest);
        
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/auctions/rooms/{id}")
                .buildAndExpand(auctionRoom.getId())
                .toUri();

        return ResponseEntity
            .created(location)
            .body(ApiResponse.<AuctionRoomResponse>success("Auction room created successfully", auctionRoom));
    }
    
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @GetMapping("/me/auctions/rooms")
    @Validated
    ResponseEntity<ApiResponse<PageResponse<AuctionRoomResponse>>> getMyAuctionRooms(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @RequestParam(defaultValue = "0") 
        @Min(value = 0, message = "Page index must not be less than 0")
        int page,

        @RequestParam(defaultValue = "10") 
        @Min(value = 1, message = "Page size must not be less than 1")
        @Max(value = 50, message = "Page size must not be greater than 50")
        int size,

        @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
        @RequestParam(required = false, defaultValue = "asc") String sortDirection
    ) {
        PageResponse<AuctionRoomResponse> myAuctionRooms = auctionRoomService.getUserAuctionRooms(
            userId, 
            page, size, 
            sortBy, sortDirection
        );

        return ResponseEntity
            .ok(ApiResponse.success("Get my auction rooms successfully", myAuctionRooms));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @GetMapping("/me/auctions/rooms/created")
    @Validated
    ResponseEntity<ApiResponse<PageResponse<AuctionRoomResponse>>> getMyCreatedAuctionRoom(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @RequestParam(defaultValue = "0") 
        @Min(value = 0, message = "Page index must not be less than 0")
        int page,

        @RequestParam(defaultValue = "10") 
        @Min(value = 1, message = "Page size must not be less than 1")
        @Max(value = 50, message = "Page size must not be greater than 50")
        int size,
        
        @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
        @RequestParam(required = false, defaultValue = "asc") String sortDirection
    ) {
        PageResponse<AuctionRoomResponse> myCreatedAuctionRoom = auctionRoomService.getOwnerCreatedAuctionRooms(
            userId, 
            page, size, 
            sortBy, sortDirection
        );

        return ResponseEntity
            .ok(ApiResponse.success("Get my created auction room successfully", myCreatedAuctionRoom));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @GetMapping("/me/auctions/rooms/{id}/joined")
    @Validated
    ResponseEntity<ApiResponse<PageResponse<AuctionRoomResponse>>> getMyJoinedAuctionRoom(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @RequestParam(defaultValue = "0") 
        @Min(value = 0, message = "Page index must not be less than 0")
        int page,

        @RequestParam(defaultValue = "10") 
        @Min(value = 1, message = "Page size must not be less than 1")
        @Max(value = 50, message = "Page size must not be greater than 50")
        int size,

        @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
        @RequestParam(required = false, defaultValue = "asc") String sortDirection
    ) {
        PageResponse<AuctionRoomResponse> myJoinedAuctionRoom = auctionRoomService.getUserJoinedAuctionRooms(
            userId, 
            page, size, 
            sortBy, sortDirection
        );

        return ResponseEntity
            .ok(ApiResponse.success("Get my joined auction room successfully", myJoinedAuctionRoom));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @PostMapping("/me/auctions/rooms/search")
    @Validated
    ResponseEntity<ApiResponse<PageResponse<AuctionRoomResponse>>> searchMyAuctionRooms(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @RequestParam(defaultValue = "0") 
        @Min(value = 0, message = "Page index must not be less than 0")
        int page,

        @RequestParam(defaultValue = "10") 
        @Min(value = 1, message = "Page size must not be less than 1")
        @Max(value = 50, message = "Page size must not be greater than 50")
        int size,
        
        @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
        @RequestParam(required = false, defaultValue = "asc") String sortDirection,
        @RequestBody(required = false) SearchAuctionRoomsRequest searchAuctionRoomsRequest
    ) {
        PageResponse<AuctionRoomResponse> myAuctionRooms = auctionRoomService.searchMyAuctionRooms(
            userId, 
            searchAuctionRoomsRequest, 
            page, size,
            sortBy, sortDirection
        );

        return ResponseEntity
            .ok(ApiResponse.success("Search my auction rooms successfully", myAuctionRooms));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @PatchMapping("/me/auctions/rooms/{id}")
    ResponseEntity<ApiResponse<AuctionRoomResponse>> updateMyAuctionRoom(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @PathVariable String id,
        @Valid @RequestBody UpdateAuctionRoomRequest updateAuctionRoomRequest
    ) {
        AuctionRoomResponse updatedAuctionRoom = auctionRoomService.updateMyAuctionRoom(userId, id, updateAuctionRoomRequest);

        return ResponseEntity
            .ok(ApiResponse.success("Update my auction room successfully", updatedAuctionRoom));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @PatchMapping("/me/auctions/rooms/{id}/cancel")
    ResponseEntity<ApiResponse<Void>> cancelMyAuctionRoom(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @PathVariable String id, 
        @Valid @RequestBody CancelAuctionRoomRequest cancelAuctionRoomRequest
    ) {
        log.info("User: [{}] want to cancel auction room: [{}]", userId, id);
        
        auctionRoomService.cancelMyAuctionRoom(userId, id, cancelAuctionRoomRequest);

        return ResponseEntity
            .ok(ApiResponse.success("Cancel my auction room successfully", null));
    }
}
