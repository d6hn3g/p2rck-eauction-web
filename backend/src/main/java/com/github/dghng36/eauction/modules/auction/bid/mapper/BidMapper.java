package com.github.dghng36.eauction.modules.auction.bid.mapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.github.dghng36.eauction.modules.auction.auctionRoom.dto.internal.AuctionRoomInfo;
import com.github.dghng36.eauction.modules.auction.bid.dto.internal.BidderInfo;
import com.github.dghng36.eauction.modules.auction.bid.dto.response.BidResponse;
import com.github.dghng36.eauction.modules.media.dto.internal.MediaFile;
import com.github.dghng36.eauction.modules.auction.bid.model.Bid;
import com.github.dghng36.eauction.modules.auction.product.dto.internal.AuctionProductInfo;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class BidMapper {
    BidderInfoMapper bidderInfoMapper;

    public List<BidResponse> toBidResponseList(List<Bid> bids, Map<String, AuctionProductInfo> auctionProductInfoMap, Map<String, AuctionRoomInfo> auctionRoomInfoMap) {
        if (bids == null) {
            return Collections.emptyList();
        }

        return bids.stream()
            .map(bid -> toBidResponse(
                bid, 
                auctionProductInfoMap.get(bid.getAuctionProductId()), 
                auctionRoomInfoMap.get(bid.getAuctionRoomId())
            ))
            .toList();
    }

    public BidResponse toBidResponse(Bid bid, AuctionProductInfo auctionProductInfo, AuctionRoomInfo auctionRoomInfo) {
         if (bid == null) {
            return null;
        }

        if (auctionProductInfo == null) {
            auctionProductInfo = defaultAuctionProductInfo();
        }

        if (auctionRoomInfo == null) {
            auctionRoomInfo = defaultAuctionRoomInfo();
        }
        
        return BidResponse.builder()
            .id(bid.getId())
            .auctionProductInfo(auctionProductInfo)
            .auctionRoomInfo(auctionRoomInfo)
            .bidderInfo(bidderInfoMapper.toBidderInfo(
                bid.getBidderInfo().getBidderId(),
                bid.getBidderInfo().getBidderName(),
                bid.getBidderInfo().getBidderAvatar()
            ))
            .bidAmount(bid.getBidAmount())
            .bidTime(bid.getBidTime())
            .isWinningBid(bid.getIsWinningBid())
            .metadata(bid.getMetadata())
            .build();
    }
    public BidderInfo toBidderInfo(String bidderId, String bidderName, MediaFile bidderAvatar) {
        return bidderInfoMapper.toBidderInfo(bidderId, bidderName, bidderAvatar);
    }

    private AuctionProductInfo defaultAuctionProductInfo() {
        return AuctionProductInfo.builder()
            .auctionProductId("N/A")
            .auctionProductName("N/A")
            .build();
    }

    private AuctionRoomInfo defaultAuctionRoomInfo() {
        return AuctionRoomInfo.builder()
            .auctionRoomId("N/A")
            .auctionRoomTitle("N/A")
            .build();
    }
}
