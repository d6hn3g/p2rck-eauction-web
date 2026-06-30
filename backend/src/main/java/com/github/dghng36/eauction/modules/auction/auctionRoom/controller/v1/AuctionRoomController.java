package com.github.dghng36.eauction.modules.auction.auctionRoom.controller.v1;

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
import com.github.dghng36.eauction.core.base.AuthInfoDto;
import com.github.dghng36.eauction.core.base.PageResponse;
import com.github.dghng36.eauction.infra.config.security.annotation.AuthInfo;
import com.github.dghng36.eauction.infra.config.security.annotation.AuthInfoType;
import com.github.dghng36.eauction.modules.auction.auctionRoom.dto.request.ParticipateAuctionRoomRequest;
import com.github.dghng36.eauction.modules.auction.auctionRoom.dto.request.SearchAuctionRoomsRequest;
import com.github.dghng36.eauction.modules.auction.auctionRoom.dto.response.AuctionRoomParticipantResponse;
import com.github.dghng36.eauction.modules.auction.auctionRoom.dto.response.AuctionRoomResponse;
import com.github.dghng36.eauction.modules.auction.auctionRoom.service.AuctionRoomService;
import com.github.dghng36.eauction.modules.identity.enums.UserRole;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;



@RestController
@RequestMapping("/api/v1/auctions/rooms")
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuctionRoomController {
    AuctionRoomService auctionRoomService;

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @GetMapping
    @Validated
    ResponseEntity<ApiResponse<PageResponse<AuctionRoomResponse>>> getAuctionRooms(
        @RequestParam(defaultValue = "0") 
        @Min(value = 0, message = "Page index must not be less than 0")
        int page,

        @RequestParam(defaultValue = "10") 
        @Min(value = 1, message = "Page size must not be less than 1")
        @Max(value = 50, message = "Page size must not be greater than 50")
        int size,

        @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
        @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        PageResponse<AuctionRoomResponse> auctionRooms = auctionRoomService.getAuctionRooms(
            page, size, 
            sortBy, sortDirection
        );

        return ResponseEntity
            .ok(ApiResponse.success("Get all auction rooms successfully", auctionRooms));
    }
    
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @PostMapping("/search")
    @Validated
    ResponseEntity<ApiResponse<PageResponse<AuctionRoomResponse>>> searchAuctionRooms(
        @RequestParam(defaultValue = "0") 
        @Min(value = 0, message = "Page index must not be less than 0")
        int page,

        @RequestParam(defaultValue = "10") 
        @Min(value = 1, message = "Page size must not be less than 1")
        @Max(value = 50, message = "Page size must not be greater than 50")
        int size,
        
        @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
        @RequestParam(required = false, defaultValue = "desc") String sortDirection,
        @RequestBody(required = false) SearchAuctionRoomsRequest searchAuctionRoomsRequest
    ) {
        PageResponse<AuctionRoomResponse> auctionRooms = auctionRoomService.searchAuctionRooms(
            searchAuctionRoomsRequest, 
            page, size,
            sortBy, sortDirection
        );

        return ResponseEntity
            .ok(ApiResponse.success("Search auction rooms successfully", auctionRooms));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @GetMapping("/{id}")
    ResponseEntity<ApiResponse<AuctionRoomResponse>> getAuctionRoom(@PathVariable String id) {
        AuctionRoomResponse auctionRoom = auctionRoomService.getAuctionRoom(id);

        return ResponseEntity
            .ok(ApiResponse.success("Get auction room by id successfully", auctionRoom));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @PostMapping("/{id}/participate")
    ResponseEntity<ApiResponse<AuctionRoomParticipantResponse>> participateAuctionRoom(
        @AuthInfo AuthInfoDto authInfo,
        @PathVariable String id, 
        @Valid @RequestBody ParticipateAuctionRoomRequest participateAuctionRoomRequest
    ) {
        String userId = authInfo.getId();
        UserRole userRole = authInfo.getRole();

        log.info("User [{}] with role [{}] is trying to participate in auction room [{}]", userId, userRole, id);

        AuctionRoomParticipantResponse participatedAuctionRoomParticipant = auctionRoomService.participateAuctionRoom(
            userId, userRole, 
            id, 
            participateAuctionRoomRequest
        );

        return ResponseEntity
            .ok(ApiResponse.success("Participated auction room successfully", participatedAuctionRoomParticipant));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @PostMapping("/{id}/leave")
    ResponseEntity<ApiResponse<AuctionRoomParticipantResponse>> leaveAuctionRoom(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @PathVariable String id
    ) {
        log.info("User [{}] is trying to leave auction room [{}]", userId, id);
        
        AuctionRoomParticipantResponse leftAuctionRoomParticipant = auctionRoomService.leaveAuctionRoom(
            userId, 
            id
        );

        return ResponseEntity
            .ok(ApiResponse.success("Leave auction room successfully", leftAuctionRoomParticipant));
    }
}

