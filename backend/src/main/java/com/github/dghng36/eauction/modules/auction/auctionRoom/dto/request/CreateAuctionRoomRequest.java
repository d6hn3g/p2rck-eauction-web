package com.github.dghng36.eauction.modules.auction.auctionRoom.dto.request;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import com.github.dghng36.eauction.core.utils.ConstantsUtils;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class CreateAuctionRoomRequest {
    // Field for creating auction room
    @NotBlank(message = "Auction room title is required")
    @Size(
        min = ConstantsUtils.AuctionConstants.MIN_AUCTION_ROOM_TITLE_LENGTH,
        max = ConstantsUtils.AuctionConstants.MAX_AUCTION_ROOM_TITLE_LENGTH,
        message = "Auction room title must be between " + ConstantsUtils.AuctionConstants.MIN_AUCTION_ROOM_TITLE_LENGTH + 
            " and " + ConstantsUtils.AuctionConstants.MAX_AUCTION_ROOM_TITLE_LENGTH + " characters"
    )
    String title;

    @NotBlank(message = "Auction room description is required")
    @Size(
        min = ConstantsUtils.AuctionConstants.MIN_AUCTION_ROOM_DESCRIPTION_LENGTH,
        max = ConstantsUtils.AuctionConstants.MAX_AUCTION_ROOM_DESCRIPTION_LENGTH,
        message = "Auction room description must be between " + ConstantsUtils.AuctionConstants.MIN_AUCTION_ROOM_DESCRIPTION_LENGTH + 
            " and " + ConstantsUtils.AuctionConstants.MAX_AUCTION_ROOM_DESCRIPTION_LENGTH + " characters"
    )
    String description;

    @Builder.Default
    @Future(message = "Auction room start time must be in the future")
    Instant startTime = Instant.now(); // Default now
    
    @Min(
        value = ConstantsUtils.AuctionConstants.MIN_AUCTION_ROOM_DURATION_MINUTES, 
        message = "Duration must be at least " + ConstantsUtils.AuctionConstants.MIN_AUCTION_ROOM_DURATION_MINUTES + " minute(s)"
    )
    @Max(
        value = ConstantsUtils.AuctionConstants.MAX_AUCTION_ROOM_DURATION_MINUTES, 
        message = "Duration must not exceed " + ConstantsUtils.AuctionConstants.MAX_AUCTION_ROOM_DURATION_MINUTES + " minutes"
    )
    @Builder.Default
    Integer durationMinutes = 60; // Default 60 minutes

    @Builder.Default
    boolean allowAutoExtend = false; // Default false

    @Min(
        value = ConstantsUtils.AuctionConstants.MIN_AUCTION_ROOM_EXTENSION_TIME, 
        message = "Extension time must be at least " + ConstantsUtils.AuctionConstants.MIN_AUCTION_ROOM_EXTENSION_TIME + " minute(s)"
    )
    @Max(
        value = ConstantsUtils.AuctionConstants.MAX_AUCTION_ROOM_EXTENSION_TIME, 
        message = "Extension time must not exceed " + ConstantsUtils.AuctionConstants.MAX_AUCTION_ROOM_EXTENSION_TIME + " minutes"
    )
    @Builder.Default
    Integer extensionTimeMinutes = 5; // Default 5 minutes if allowAutoExtend is true

    @Size(
        max = ConstantsUtils.MetadataConstants.MAX_METADATA_SIZE,
        message = "Too many metadata entries"
    )
    @Builder.Default
    Map<String, Object> metadata = new LinkedHashMap<>();

    @Builder.Default
    boolean chatEnabled = true; // Default true

    @Min(
        value = ConstantsUtils.AuctionConstants.MIN_AUCTION_ROOM_PARTICIPANTS, message = "Total participants must be at least " + ConstantsUtils.AuctionConstants.MIN_AUCTION_ROOM_PARTICIPANTS
    )
    @Max(
        value = ConstantsUtils.AuctionConstants.MAX_AUCTION_ROOM_PARTICIPANTS, message = "Total participants must not exceed " + ConstantsUtils.AuctionConstants.MAX_AUCTION_ROOM_PARTICIPANTS
    )
    @Builder.Default
    Integer totalParticipants = 50; // Default 50

    // Field for creating auction product
    @NotBlank(message = "Product ID is required")
    String productId;

    @NotNull(message = "Start price is required")
    @Positive(message = "Start price must be greater than 0")
    Double startPrice;

    @NotNull(message = "Price step is required")
    @Positive(message = "Price step must be greater than 0")
    Double priceStep;

    @NotNull(message = "Buyout price is required")
    @Positive(message = "Buyout price must be greater than 0")
    Double buyoutPrice;
}
