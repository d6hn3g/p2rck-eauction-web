package com.github.dghng36.eauction.modules.auction.bid.service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.github.dghng36.eauction.modules.auction.bid.dto.internal.AutoBidResult;
import com.github.dghng36.eauction.modules.auction.bid.model.AutoBidSetting;

@Service
public class AutoBidEngine {
     public AutoBidResult resolverWinner(
        String auctionRoomId,
        Double currentBidAmount,
        List<AutoBidSetting> autoBidSettings
     ) {
        autoBidSettings.removeIf(Objects::isNull);

        if (autoBidSettings.isEmpty()) {
            return AutoBidResult.builder()
                .hasAutoBid(false)
                .finalPrice(currentBidAmount)
                .build();
        }

        // Sorting desc
        autoBidSettings.sort(
            Comparator.comparing(
                (AutoBidSetting setting) -> setting.getMaxAutoBidPrice()
            ).reversed()
        );
        AutoBidSetting topAutoBidSetting = autoBidSettings.get(0);

        if (autoBidSettings.size() == 1) {
            Double nextPrice = currentBidAmount + topAutoBidSetting.getIncrementAmount();

            if (nextPrice > topAutoBidSetting.getMaxAutoBidPrice()) {
                return AutoBidResult.builder()
                    .hasAutoBid(false)
                    .finalPrice(currentBidAmount)
                    .build();
            }

            return AutoBidResult.builder()
                .hasAutoBid(true)
                .winnerUserId(topAutoBidSetting.getUserId())
                .finalPrice(nextPrice)
                .build();
        }

        AutoBidSetting secondAutoBidSetting = autoBidSettings.get(1);
        Double finalPrice = Math.min(
            secondAutoBidSetting.getMaxAutoBidPrice() + topAutoBidSetting.getIncrementAmount(),
            topAutoBidSetting.getMaxAutoBidPrice()
        );

        if (finalPrice <= currentBidAmount) {
            return AutoBidResult.builder()
                .hasAutoBid(false)
                .finalPrice(currentBidAmount)
                .build();
        }

        return AutoBidResult.builder()
            .hasAutoBid(true)
            .winnerUserId(topAutoBidSetting.getUserId())
            .finalPrice(finalPrice)
            .build();

    }
}
