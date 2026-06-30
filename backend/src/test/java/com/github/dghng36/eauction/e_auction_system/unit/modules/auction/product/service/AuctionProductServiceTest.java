package com.github.dghng36.eauction.e_auction_system.unit.modules.auction.product.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import org.bson.Document;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.github.dghng36.eauction.modules.auction.enums.ProductStatus;
import com.github.dghng36.eauction.modules.auction.product.dto.internal.AuctionProduct;
import com.github.dghng36.eauction.modules.auction.product.dto.internal.AuctionProductInfo;
import com.github.dghng36.eauction.modules.auction.product.mapper.AuctionProductInfoMapper;
import com.github.dghng36.eauction.modules.auction.product.mapper.AuctionProductMapper;
import com.github.dghng36.eauction.modules.auction.product.model.Product;
import com.github.dghng36.eauction.modules.auction.product.repository.ProductRepository;
import com.github.dghng36.eauction.modules.auction.product.service.AuctionProductService;
import com.github.dghng36.eauction.modules.media.dto.internal.MediaFile;
import com.mongodb.client.result.UpdateResult;

@ExtendWith(MockitoExtension.class)
public class AuctionProductServiceTest {
    @Mock private MongoTemplate mongoTemplate;
    @Mock private ProductRepository productRepo;
    @Mock private AuctionProductMapper auctionProductMapper;
    @Mock private AuctionProductInfoMapper auctionProductInfoMapper;

    @InjectMocks private AuctionProductService auctionProductService;

    private Product mockProduct;
    private final String productId = "product-id-123"; 

    @BeforeEach
    void setUp() {
        mockProduct = Product.builder()
            .id(productId)
            .name("Mock product test")
            .description("This mock product description")
            .images(List.of(
                MediaFile.builder()
                    .mediaCode("media-file-id-123")
                    .objectKey("Mock object key")
                    .originalUrl("https://mock-test-url")
                    .build()
            ))
            .build();
    }

    /**
     * Test cases for createAuctionProduct
     * Tests:
     * - createAuctionProduct_ProductExists_ShouldReturnAuctionProduct
     * - createAuctionProduct_ProductDoesNotExist_ShouldReturnNull
     */

    @Test
    void createAuctionProduct_ProductExists_ShouldReturnAuctionProduct() {
        // Arrange
        AuctionProduct expectedAuctionProduct = AuctionProduct.builder()
            .productId(productId)
            .build();
        when(productRepo.findByIdAndIsDeletedFalse(productId)).thenReturn(Optional.of(mockProduct));
        when(auctionProductMapper.toAuctionProduct(
            any(), 
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any()
        )).thenReturn(expectedAuctionProduct);

        // Act
        AuctionProduct actualAuctionProduct = auctionProductService.createAuctionProduct(
            productId, 100.0, 10.0, 500.0
        );

        // Assert
        assertNotNull(actualAuctionProduct);
        verify(auctionProductMapper, times(1)).toAuctionProduct(eq(productId), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void createAuctionProduct_ProductDoesNotExist_ShouldReturnNull() {
        // Arrange
        when(productRepo.findByIdAndIsDeletedFalse(productId)).thenReturn(Optional.empty());

        // Act
        AuctionProduct actualAuctionProduct = auctionProductService.createAuctionProduct(
            productId, 100.0, 10.0, 500.0
        );

        // Assert
        assertNull(actualAuctionProduct);
        verify(auctionProductMapper, never()).toAuctionProduct(any(), any(), any(), any(), any(), any(), any(), any());
    }

    /**
     * Test cases for getAuctionProductInfoByIds
     * Tests:
     * - getAuctionProductInfoByIds_ProductsExist_ShouldReturnInfoMap
     * - getAuctionProductInfoByIds_NoProductsExist_ShouldReturnEmptyMap
     */
    
    @Test
    void getAuctionProductInfoByIds_ProductsExist_ShouldReturnInfoMap() {
        // Arrange
        when(productRepo.findAllByIdInAndIsDeletedFalse(any())).thenReturn(List.of(mockProduct));
        when(auctionProductInfoMapper.toAuctionProductInfo(any(), any())).thenReturn(
            AuctionProductInfo.builder()
                .auctionProductId(productId)
                .auctionProductName(mockProduct.getName())
                .build()
        );

        // Act
        Map<String, AuctionProductInfo> infoMap = auctionProductService.getAuctionProductInfoByIds(Set.of(productId));

        // Assert
        assertNotNull(infoMap);
        assert(infoMap.containsKey(productId));
        verify(auctionProductInfoMapper, times(1)).toAuctionProductInfo(eq(productId), any());
    }

    @Test
    void getAuctionProductInfoByIds_NoProductsExist_ShouldReturnEmptyMap() {
        // Arrange
        when(productRepo.findAllByIdInAndIsDeletedFalse(any())).thenReturn(List.of());

        // Act
        Map<String, AuctionProductInfo> infoMap = auctionProductService.getAuctionProductInfoByIds(Set.of(productId));

        // Assert
        assertNotNull(infoMap);
        assert(infoMap.isEmpty());
        verify(auctionProductInfoMapper, never()).toAuctionProductInfo(any(), any());
    }

    /**
     * Test cases for markProduct
     * Tests:
     * - markProductInAuction_ProductExists_ShouldUpdateStatus
     * - markProductInAuction_ProductDoesNotExist_ShouldDoNothing
     */

    @Test
    void markProductInAuction_ProductExists_ShouldUpdateStatus() {
        UpdateResult mockUpdateResult = UpdateResult.acknowledged(1L, 1L, null);
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Product.class)))
                .thenReturn(mockUpdateResult);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);

        // Act
        auctionProductService.markProductInAuction(productId);

        // Assert & Verify
        verify(mongoTemplate, times(1)).updateFirst(queryCaptor.capture(), updateCaptor.capture(), eq(Product.class));

        String capturedQueryJson = queryCaptor.getValue().getQueryObject().toJson();
        assertThat(capturedQueryJson).contains(productId);
        assertThat(capturedQueryJson).contains("\"isDeleted\": false");

        Document updateDoc = updateCaptor.getValue().getUpdateObject();
        assertTrue(updateDoc.containsKey("$set"));
        Document setFields = (org.bson.Document) updateDoc.get("$set");
        assertThat(setFields.get("status")).isEqualTo(ProductStatus.IN_AUCTION.name());
    }

    @Test
    void markProductInAuction_ProductDoesNotExist_ShouldDoNothing() {
        UpdateResult mockUpdateResult = UpdateResult.acknowledged(0L, 0L, null);
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Product.class)))
                .thenReturn(mockUpdateResult);

        // Act
        auctionProductService.markProductInAuction(productId);

        // Assert
        verify(mongoTemplate, times(1)).updateFirst(any(Query.class), any(Update.class), eq(Product.class));
    }
}
