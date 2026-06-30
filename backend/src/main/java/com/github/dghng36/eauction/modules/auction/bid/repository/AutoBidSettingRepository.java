package com.github.dghng36.eauction.modules.auction.bid.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.github.dghng36.eauction.modules.auction.bid.model.AutoBidSetting;

public interface AutoBidSettingRepository extends MongoRepository<AutoBidSetting, String> {
    Optional<AutoBidSetting> findByAuctionRoomIdAndUserId(String auctionRoomId, String userId);

    List<AutoBidSetting> findByAuctionRoomIdAndEnabledTrueAndUserIdNot(String auctionRoomId, String userId);
}
