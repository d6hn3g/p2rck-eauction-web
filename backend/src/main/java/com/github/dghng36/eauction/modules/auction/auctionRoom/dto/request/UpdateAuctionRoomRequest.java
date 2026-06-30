package com.github.dghng36.eauction.modules.auction.auctionRoom.dto.request;

import java.time.Instant;
import java.util.Map;

import com.github.dghng36.eauction.core.utils.ConstantsUtils;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
public class UpdateAuctionRoomRequest {
    String title;

    @Size(
        min = ConstantsUtils.AuctionConstants.MIN_AUCTION_ROOM_DESCRIPTION_LENGTH, 
        max = ConstantsUtils.AuctionConstants.MAX_AUCTION_ROOM_DESCRIPTION_LENGTH, 
        message = "Auction room description must be between " + ConstantsUtils.AuctionConstants.MIN_AUCTION_ROOM_DESCRIPTION_LENGTH + 
            " and " + ConstantsUtils.AuctionConstants.MAX_AUCTION_ROOM_DESCRIPTION_LENGTH + " characters"
    )
    String description;

    Boolean allowAutoExtend;
    Integer newDurationExtensionTime;

    Instant newStartTime;

    @Min(
        value = ConstantsUtils.AuctionConstants.MIN_AUCTION_ROOM_DURATION_MINUTES, 
        message = "Duration must be at least " + ConstantsUtils.AuctionConstants.MIN_AUCTION_ROOM_DURATION_MINUTES + " minute(s)"
    )
    @Max(
        value = ConstantsUtils.AuctionConstants.MAX_AUCTION_ROOM_DURATION_MINUTES, 
        message = "Duration must not exceed " + ConstantsUtils.AuctionConstants.MAX_AUCTION_ROOM_DURATION_MINUTES + " minutes"
    )
    Integer newDurationMinutes;

    @Size(
        max = ConstantsUtils.MetadataConstants.MAX_METADATA_SIZE,
        message = "Too many metadata entries"
    )
    Map<String, Object> newMetadata;

    Integer newAmountTotalParticipants;

    Boolean chatEnabled;
}
