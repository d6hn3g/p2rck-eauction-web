package com.github.dghng36.eauction.modules.auction.auctionRoom.dto.request;

import com.github.dghng36.eauction.core.utils.ConstantsUtils;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class ParticipateAuctionRoomRequest {
    @Size(
        max = ConstantsUtils.AuctionConstants.MAX_REASON_LENGTH, 
        message = "Participated reason must not exceed " + ConstantsUtils.AuctionConstants.MAX_REASON_LENGTH + " characters"
    )
    String participatedReason;
}
