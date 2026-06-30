package com.github.dghng36.eauction.modules.auction.product.dto.response;

import com.github.dghng36.eauction.modules.media.dto.internal.MediaFile;

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
public class AuctionProductResponse {
    String productId;
    String productName;
    String productDescription;

    MediaFile mainImage;

    Double startPrice;
    Double priceStep;
    Double currentPrice;
    Double buyoutPrice;

}
