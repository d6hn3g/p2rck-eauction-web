package com.github.dghng36.eauction.modules.auction.auctionRoom.controller.v1;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.dghng36.eauction.core.base.ApiResponse;
import com.github.dghng36.eauction.core.base.AuthInfoDto;
import com.github.dghng36.eauction.core.base.PageResponse;
import com.github.dghng36.eauction.infra.config.security.annotation.AuthInfo;
import com.github.dghng36.eauction.infra.config.security.annotation.AuthInfoType;
import com.github.dghng36.eauction.modules.auction.auctionRoom.dto.request.UpdateAuctionRoomStatusRequest;
import com.github.dghng36.eauction.modules.auction.auctionRoom.dto.request.UpdateParticipantStatusRequest;
import com.github.dghng36.eauction.modules.auction.auctionRoom.dto.response.AuctionRoomParticipantResponse;
import com.github.dghng36.eauction.modules.auction.auctionRoom.dto.response.AuctionRoomResponse;
import com.github.dghng36.eauction.modules.auction.auctionRoom.service.AuctionRoomService;
import com.github.dghng36.eauction.modules.identity.enums.UserRole;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/manager/management/auctions/rooms")
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ManagerAuctionRoomController {
    AuctionRoomService auctionRoomService;

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping
    @Validated
    ResponseEntity<ApiResponse<PageResponse<AuctionRoomResponse>>> getManagedAuctionRooms(
        @AuthInfo AuthInfoDto authInfoDto,
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
        String managerId = authInfoDto.getId();
        UserRole managerRole = authInfoDto.getRole();

        PageResponse<AuctionRoomResponse> managedAuctionRooms = auctionRoomService.getManagedAuctionRooms(
            managerId, managerRole, 
            page, size,
            sortBy, sortDirection
        );

        return ResponseEntity
            .ok(ApiResponse.success("Get managed auction rooms successfully", managedAuctionRooms));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PatchMapping("/{id}")
    ResponseEntity<ApiResponse<AuctionRoomResponse>> updateManagerAuctionRoom(
        @AuthInfo AuthInfoDto authInfoDto,
        @PathVariable String id
    ) {
        String managerId = authInfoDto.getId();
        UserRole managerRole = authInfoDto.getRole();

        log.info("Update managed auction room with manager: [{}], role: [{}]", managerId, managerRole);

        AuctionRoomResponse updatedAuctionRoom = auctionRoomService.updateManagerAuctionRoom(managerId, managerRole, id);

        return ResponseEntity
            .ok(ApiResponse.success("Update manager room successfully", updatedAuctionRoom));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PatchMapping("/{id}/status")
    ResponseEntity<ApiResponse<AuctionRoomResponse>> updateAuctionRoomStatus(
        @AuthInfo(info = AuthInfoType.ID) String managerId,
        @PathVariable String id,
        @RequestBody UpdateAuctionRoomStatusRequest updateAuctionRoomStatusRequest
    ) {
        log.info("Update new status for auction room: [{}] by manager: [{}]", id, managerId);

        AuctionRoomResponse updatedAuctionRoom = auctionRoomService.updateAuctionRoomStatus(
            managerId,
            id, 
            updateAuctionRoomStatusRequest
        );

        return ResponseEntity
            .ok(ApiResponse.success("Update auction room status successfully", updatedAuctionRoom));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PatchMapping("/{id}/participants/{userId}/status")
    ResponseEntity<ApiResponse<AuctionRoomParticipantResponse>> updateParticipantStatus(
        @AuthInfo(info = AuthInfoType.ID) String managerId,
        @PathVariable String id,
        @PathVariable String userId,
        @RequestBody UpdateParticipantStatusRequest updateParticipantStatusRequest
    ) {
        log.info("Update new participant status for auction room: [{}] by manager: [{}] to user: [{}]", id, managerId, userId);

        AuctionRoomParticipantResponse updatedAuctionRoom = auctionRoomService.updateAuctionRoomParticipantStatus(
            managerId,
            id, 
            userId, 
            updateParticipantStatusRequest
        );

        return ResponseEntity
            .ok(ApiResponse.success("Update participant status successfully", updatedAuctionRoom));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @DeleteMapping("/{id}")
    ResponseEntity<ApiResponse<Void>> deleteAuctionRoom(
        @AuthInfo(info = AuthInfoType.ID) String managerId, 
        @PathVariable String id
    ) { 
        auctionRoomService.deleteAuctionRoom(managerId, id);

        return ResponseEntity
            .ok(ApiResponse.success("Delete auction room successfully", null));
    }

}
