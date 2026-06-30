package com.github.dghng36.eauction.modules.auction.product.mapper;

import org.springframework.stereotype.Component;

import com.github.dghng36.eauction.modules.auction.product.dto.internal.AuctionProduct;
import com.github.dghng36.eauction.modules.auction.product.dto.response.AuctionProductResponse;
import com.github.dghng36.eauction.modules.media.dto.internal.MediaFile;

@Component
public class AuctionProductMapper {
    public AuctionProduct toAuctionProduct(
        String productId,
        String productName,
        String productDescription,
        MediaFile mainImage,
        Double startPrice,
        Double priceStep,
        Double currentPrice,
        Double buyoutPrice
    ) {
        if (productId == null || productName == null) {
            return null;
        }

        return AuctionProduct.builder()
            .productId(productId)
            .productName(productName)
            .productDescription(productDescription)
            .mainImage(mainImage)
            .startPrice(startPrice)
            .priceStep(priceStep)
            .currentPrice(currentPrice)
            .buyoutPrice(buyoutPrice)
            .build();
    }

    public AuctionProductResponse toAuctionProductResponse(AuctionProduct auctionProduct) {
        if (auctionProduct == null) {
            return null;
        }

        return AuctionProductResponse.builder()
            .productId(auctionProduct.getProductId())
            .productName(auctionProduct.getProductName())
            .productDescription(auctionProduct.getProductDescription())
            .mainImage(auctionProduct.getMainImage())
            .startPrice(auctionProduct.getStartPrice())
            .priceStep(auctionProduct.getPriceStep())
            .currentPrice(auctionProduct.getCurrentPrice())
            .buyoutPrice(auctionProduct.getBuyoutPrice())
            .build();
    }
}
