package com.github.dghng36.eauction.modules.auction.product.dto.response;

import java.util.List;
import java.util.Map;

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
public class ProductResponse {
    String id;

    String name;
    String description;

    String ownerId;

    List<MediaFile> images;

    Map<String, Object> metadata;

    String status;
}
