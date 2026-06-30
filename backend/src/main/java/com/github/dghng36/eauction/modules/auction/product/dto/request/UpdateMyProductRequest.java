package com.github.dghng36.eauction.modules.auction.product.dto.request;

import java.util.List;
import java.util.Map;

import com.github.dghng36.eauction.core.utils.ConstantsUtils;
import com.github.dghng36.eauction.modules.media.dto.request.MediaFileUploadRequest;

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
public class UpdateMyProductRequest {
    @Size(
        min = ConstantsUtils.AuctionConstants.MIN_PRODUCT_NAME_LENGTH, 
        max = ConstantsUtils.AuctionConstants.MAX_PRODUCT_NAME_LENGTH, 
        message = "Product name must be between " + ConstantsUtils.AuctionConstants.MIN_PRODUCT_NAME_LENGTH + 
            " and " + ConstantsUtils.AuctionConstants.MAX_PRODUCT_NAME_LENGTH + " characters"
    )
    String name;

    @Size(
        min = ConstantsUtils.AuctionConstants.MIN_PRODUCT_DESCRIPTION_LENGTH, 
        max = ConstantsUtils.AuctionConstants.MAX_PRODUCT_DESCRIPTION_LENGTH, 
        message = "Product description must be between " + ConstantsUtils.AuctionConstants.MIN_PRODUCT_DESCRIPTION_LENGTH + 
            " and " + ConstantsUtils.AuctionConstants.MAX_PRODUCT_DESCRIPTION_LENGTH + " characters"
    )
    String description;

    @Size(
        max = ConstantsUtils.MediaFileConstants.MAX_MEDIA_FILE_URL, 
        message = "At most " + ConstantsUtils.MediaFileConstants.MAX_MEDIA_FILE_URL + " media files are allowed"
    )
    List<MediaFileUploadRequest> images;

    @Size(
        max = ConstantsUtils.MetadataConstants.MAX_METADATA_SIZE, 
        message = "At most " + ConstantsUtils.MetadataConstants.MAX_METADATA_SIZE + " metadata entries are allowed"
    )
    Map<String, Object> metadata;
}
