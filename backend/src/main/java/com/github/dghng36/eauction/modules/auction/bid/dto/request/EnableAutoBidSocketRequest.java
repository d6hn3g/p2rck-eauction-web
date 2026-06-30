package com.github.dghng36.eauction.modules.auction.bid.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class EnableAutoBidSocketRequest {
    @NotBlank(message = "Auction room id is required")
    String auctionRoomId;

    @NotNull(message = "Max auto bid price is required")
    @Positive(message = "Max auto bid price must be greater than 0")
    Double maxAutoBidPrice;

    @NotNull(message = "Increment amount is required")
    @Positive(message = "Increment amount must be greater than 0")
    Double incrementAmount;
}
