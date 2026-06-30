package com.github.dghng36.eauction.modules.auction.bid.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.modules.auction.bid.dto.internal.AutoBidResult;
import com.github.dghng36.eauction.modules.auction.bid.model.AutoBidSetting;
import com.github.dghng36.eauction.modules.auction.bid.repository.AutoBidSettingRepository;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AutoBidResolver {
    AutoBidSettingRepository autoBidSettingRepo;

    BidService bidService;

    AutoBidEngine autoBidEngine;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void processAutoBid(
        String auctionRoomId,
        String currentBidderId,
        Double currentBidAmount
    ) {
        List<AutoBidSetting> activeAutoBidSettings = autoBidSettingRepo
            .findByAuctionRoomIdAndEnabledTrueAndUserIdNot(auctionRoomId, currentBidderId);

        if (activeAutoBidSettings == null || activeAutoBidSettings.isEmpty()) {
            return;
        }

        AutoBidResult autoBidResult = autoBidEngine.resolverWinner(
            auctionRoomId,
            currentBidAmount,
            activeAutoBidSettings
        );

        if (autoBidResult == null || !autoBidResult.getHasAutoBid()) {
            return;
        }

        if (autoBidResult.getFinalPrice() <= currentBidAmount) {
            log.warn("Auto-bid robot guard triggered: Final price [{}] is not higher than current amount: [{}]. Aborting to prevent infinite loop.", 
                autoBidResult.getFinalPrice(), currentBidAmount);
            return;
        }

        log.info("Auto-bid Robot: [{}] is executing auto-bid with price: [{}] in auction room: [{}]", 
            autoBidResult.getWinnerUserId(), autoBidResult.getFinalPrice(), auctionRoomId);

        try {
            bidService.placeBid(
                autoBidResult.getWinnerUserId(),
                auctionRoomId,
                autoBidResult.getFinalPrice(),
                null,
                false,
                null,
                null,
                true
            );
        } catch (AppException ex) {
            log.warn("Auto-bid Robot bid missed the window or failed: [{}], error: [{}]: ", ex.getMessage(), ex);
        } catch (Exception ex) {
            log.error("Critical error during AutoBid execution: [{}]", ex.getMessage(), ex);
        }
    }
}
