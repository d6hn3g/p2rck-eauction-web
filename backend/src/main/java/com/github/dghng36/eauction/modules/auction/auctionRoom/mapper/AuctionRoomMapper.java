package com.github.dghng36.eauction.modules.auction.auctionRoom.mapper;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.github.dghng36.eauction.modules.auction.auctionRoom.dto.response.AuctionRoomResponse;
import com.github.dghng36.eauction.modules.auction.auctionRoom.model.AuctionRoom;
import com.github.dghng36.eauction.modules.auction.enums.AuctionRoomStatus;
import com.github.dghng36.eauction.modules.auction.product.dto.internal.AuctionProduct;
import com.github.dghng36.eauction.modules.auction.product.mapper.AuctionProductMapper;
import com.github.dghng36.eauction.modules.identity.user.dto.internal.UserInfo;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class AuctionRoomMapper {
    AuctionProductMapper auctionProductMapper;

    public AuctionRoom toAuctionRoomEntity(
        String title, String description,
        String ownerId, AuctionProduct auctionProduct, 
        Double startingPrice,
        Instant startTime, Instant endTime,
        boolean allowAutoExtend, int extensionTime,
        boolean chatEnabled,
        Map<String, Object> metadata,
        int totalParticipants
    ) {
        return AuctionRoom.builder()
            .title(title)
            .description(description)
            .ownerId(ownerId)
            .auctionProduct(auctionProduct)
            .currentMaxPrice(startingPrice)
            .startTime(startTime)
            .endTime(endTime)
            .allowAutoExtend(allowAutoExtend)
            .extensionTime(extensionTime)
            .chatEnabled(chatEnabled)
            .metadata(metadata)
            .status(AuctionRoomStatus.PENDING_VERIFIED)
            .currentParticipants(0)
            .totalParticipants(totalParticipants)
            .isDeleted(false)
            .deletedAt(null)
            .build();
    }

    public AuctionRoomResponse toAuctionRoomResponse(
        AuctionRoom auctionRoom,
        Map<String, UserInfo> userInfoMaps
    ) {
        if (auctionRoom == null) {
            return null;
        }

        UserInfo ownerInfo = userInfoMaps.get(auctionRoom.getOwnerId());
        UserInfo managerInfo = userInfoMaps.get(auctionRoom.getManagerId());

        return AuctionRoomResponse.builder()
            .id(auctionRoom.getId())
            .title(auctionRoom.getTitle())
            .description(auctionRoom.getDescription())
            .auctionProductResponse(auctionProductMapper.toAuctionProductResponse(auctionRoom.getAuctionProduct()))
            .owner(ownerInfo)
            .manager(managerInfo)
            .startTime(auctionRoom.getStartTime())
            .endTime(auctionRoom.getEndTime())
            .isExtended(auctionRoom.getAllowAutoExtend() && auctionRoom.getEndTime().isAfter(Instant.now()))
            .extensionTime(auctionRoom.getExtensionTime())
            .status(auctionRoom.getStatus().name())
            .metadata(auctionRoom.getMetadata())
            .chatEnabled(auctionRoom.isChatEnabled())
            .conversationId(auctionRoom.getConversationId())
            .cancelReason(auctionRoom.getCancelReason())
            .canceledAt(auctionRoom.getCanceledAt())
            .build();
    }

    public List<AuctionRoomResponse> toAuctionRoomResponseList(
        List<AuctionRoom> auctionRooms,
        Map<String, UserInfo> userInfoMaps
    ) {
        return auctionRooms.stream()
            .map(auctionRoom -> toAuctionRoomResponse(auctionRoom, userInfoMaps))
            .toList();
    }
}
