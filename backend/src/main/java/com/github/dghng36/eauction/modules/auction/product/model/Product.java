package com.github.dghng36.eauction.modules.auction.product.model;

import java.util.List;
import java.util.Map;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.github.dghng36.eauction.core.base.BaseEntity;
import com.github.dghng36.eauction.modules.auction.enums.ProductStatus;
import com.github.dghng36.eauction.modules.media.dto.internal.MediaFile;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Document(collection = "products")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class Product extends BaseEntity{
    String name;

    String description;

    @Indexed
    String ownerId;
    
    List<MediaFile> images;

    Map<String, Object> metadata;

    @Indexed
    ProductStatus status;
}
