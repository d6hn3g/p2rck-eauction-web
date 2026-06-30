package com.github.dghng36.eauction.modules.auction.bid.service;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.github.dghng36.eauction.modules.auction.bid.model.AutoBidSetting;
import com.github.dghng36.eauction.modules.auction.bid.repository.AutoBidSettingRepository;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class AutoBidProcessor {
    MongoTemplate mongoTemplate;
    AutoBidSettingRepository autoBidSettingRepo;

    public void processEnableAutoBid(
        String auctionRoomId, String userId,
        Double maxAutoBidPrice, Double incrementAmount
    ) { 
        // Check if enable auto bid for user in auction room
        Query query = new Query().addCriteria(
            Criteria.where("auctionRoomId").is(auctionRoomId)
                .and("userId").is(userId)
        );

        Update update = new Update()
            .set("enabled", true)
            .set("maxAutoBidPrice", maxAutoBidPrice)
            .set("incrementAmount", incrementAmount);

        mongoTemplate.upsert(query, update, AutoBidSetting.class);
    }

    public void processDisableAutoBid(String auctionRoomId, String userId) {
        AutoBidSetting autoBidSetting = autoBidSettingRepo.findByAuctionRoomIdAndUserId(auctionRoomId, userId)
            .orElse(null);
        if (autoBidSetting != null) {
            autoBidSetting.setEnabled(false);

            autoBidSettingRepo.save(autoBidSetting);
        }
    }

    public boolean validateAutoBidPrice(Double maxAutoBidPrice, Double incrementAmount) {
        return maxAutoBidPrice != null && maxAutoBidPrice > 0
            && incrementAmount != null && incrementAmount > 0;
    }
}
