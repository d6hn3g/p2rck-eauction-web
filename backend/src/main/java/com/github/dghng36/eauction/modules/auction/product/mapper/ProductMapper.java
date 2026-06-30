package com.github.dghng36.eauction.modules.auction.product.mapper;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.github.dghng36.eauction.core.utils.MetadataUtils;
import com.github.dghng36.eauction.modules.auction.enums.ProductStatus;
import com.github.dghng36.eauction.modules.auction.product.dto.request.CreateProductRequest;
import com.github.dghng36.eauction.modules.auction.product.dto.request.UpdateMyProductRequest;
import com.github.dghng36.eauction.modules.auction.product.dto.response.ProductResponse;
import com.github.dghng36.eauction.modules.auction.product.model.Product;
import com.github.dghng36.eauction.modules.media.dto.internal.MediaFile;

@Component
public class ProductMapper {
    public Product toProductEntity(
        String ownerId, 
        CreateProductRequest createProductRequest, 
        List<MediaFile> images
    ) {
        if (ownerId == null || createProductRequest == null) {
            return null;
        }

        return Product.builder()
            .name(createProductRequest.getName())
            .description(createProductRequest.getDescription())
            .ownerId(ownerId)
            .images(images)
            .metadata(
                MetadataUtils.sanitizeDynamicMetadata(createProductRequest.getMetadata())
            )
            .status(ProductStatus.AVAILABLE)
            .isDeleted(false)
            .deletedAt(null)
            .build();
    }

    public ProductResponse toProductResponse(Product product) {
        if (product == null) {
            return null;
        }

        return ProductResponse.builder()
            .id(product.getId())
            .name(product.getName())
            .description(product.getDescription())
            .ownerId(product.getOwnerId())
            .images(product.getImages())
            .metadata(product.getMetadata())
            .status(product.getStatus().name())
            .build();
    }

    public List<ProductResponse> toProductResponseList(List<Product> products) {
        if (products == null) {
            return List.of();
        }

        return products.stream()
            .map(this::toProductResponse)
            .toList();
    }

    public void updateProductEntity(Product product, UpdateMyProductRequest updateMyProductRequest) {
        if (product == null || updateMyProductRequest == null) {
            return;
        }

        if (StringUtils.hasText(updateMyProductRequest.getName())
            && !updateMyProductRequest.getName().equals(product.getName())
        ) {
            product.setName(updateMyProductRequest.getName());
        }
        
        if (StringUtils.hasText(updateMyProductRequest.getDescription())
            && !updateMyProductRequest.getDescription().equals(product.getDescription())
        ) {
            product.setDescription(updateMyProductRequest.getDescription());
        }

        /**
         * Images and metadata are handled separately in the service layer, 
         * so we don't update them here to avoid unintended side effects.
         */
    }
}
