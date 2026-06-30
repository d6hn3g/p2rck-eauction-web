package com.github.dghng36.eauction.modules.auction.bid.dto.request;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class PlaceBidSocketRequest {
    
    @NotBlank(
        message = "Auction room ID is required"
    )
    String auctionRoomId;

    @NotNull(
        message = "Bid amount is required"
    )
    @Positive(message = "Bid amount must be greater than 0")
    Double bidAmount;

    @Builder.Default
    Map<String, Object> metadata = new LinkedHashMap<>();

    // Auto bid fields if user need
    Boolean enableAutoBid;

    @Positive(message = "Max auto bid price must be greater than 0")
    Double maxAutoBidPrice;

    @Positive(message = "Amount bid must be greater than 0")
    Double incrementAmount;
}

