package com.github.dghng36.eauction.modules.auction.product.controller.v1;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.github.dghng36.eauction.core.base.ApiResponse;
import com.github.dghng36.eauction.core.base.PageResponse;
import com.github.dghng36.eauction.infra.config.security.annotation.AuthInfo;
import com.github.dghng36.eauction.infra.config.security.annotation.AuthInfoType;
import com.github.dghng36.eauction.modules.auction.product.dto.request.CreateProductRequest;
import com.github.dghng36.eauction.modules.auction.product.dto.request.SearchProductsRequest;
import com.github.dghng36.eauction.modules.auction.product.dto.request.UpdateMyProductRequest;
import com.github.dghng36.eauction.modules.auction.product.dto.response.ProductResponse;
import com.github.dghng36.eauction.modules.auction.product.service.ProductService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/users/me/products")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class MyProductController {
    ProductService productService;

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @PostMapping
    ResponseEntity<ApiResponse<ProductResponse>> createMyProduct(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @Valid @RequestBody CreateProductRequest createProductRequest 
    ) {  
        log.info("Creating product for user: [{}], with product name: {}", userId, createProductRequest.getName());
        
        ProductResponse newProduct = productService.createMyProduct(userId, createProductRequest);

        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/api/v1/products/{id}")
            .buildAndExpand(newProduct.getId())
            .toUri();

        return ResponseEntity.created(location)
            .body(ApiResponse.success("Product created successfully", newProduct));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @GetMapping
    @Validated
    ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getMyProducts(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @RequestParam(defaultValue = "0") 
        @Min(value = 0, message = "Page index must not be less than 0") 
        int page,

        @RequestParam(defaultValue = "10") 
        @Min(value = 1, message = "Page size must not be less than 1")
        @Max(value = 50, message = "Page size must not be greater than 50")
        int size,

        @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
        @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        PageResponse<ProductResponse> productsPage = productService.getMyProducts(
            userId, 
            page, size, 
            sortBy, sortDirection
        );
        
        return ResponseEntity.ok(ApiResponse.success("Get all my products successfully", productsPage));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @GetMapping("/{id}")
    ResponseEntity<ApiResponse<ProductResponse>> getMyProduct(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @PathVariable String id
    ) {
        ProductResponse product = productService.getMyProduct(userId, id);
        
        return ResponseEntity.ok(ApiResponse.success("Get my product successfully", product));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @PostMapping("/search")
    @Validated
    ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> searchMyProducts(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @RequestBody SearchProductsRequest searchMyProductsRequest,
        @RequestParam(defaultValue = "0") 
        @Min(value = 0, message = "Page index must not be less than 0")
        int page,

        @RequestParam(defaultValue = "10") 
        @Min(value = 1, message = "Page size must not be less than 1") 
        @Max(value = 50, message = "Page size must not be greater than 50")
        int size,
        
        @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
        @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        PageResponse<ProductResponse> productsPage = productService.searchMyProducts(
            userId, 
            searchMyProductsRequest, 
            page, size, 
            sortBy, sortDirection
        );
        
        return ResponseEntity.ok(ApiResponse.success("Search my products successfully", productsPage));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @PatchMapping("/{id}")
    ResponseEntity<ApiResponse<ProductResponse>> updateMyProduct(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @PathVariable String id,
        @Valid @RequestBody UpdateMyProductRequest updateProductRequest
    ) {
        log.info("Updating product with id: [{}] for user: [{}]", id, userId);
        
        ProductResponse updatedProduct = productService.updateMyProduct(userId, id, updateProductRequest);
        
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", updatedProduct));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @DeleteMapping("/{id}")
    ResponseEntity<ApiResponse<Void>> deleteMyProduct(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @PathVariable String id
    ) {
        log.info("Deleting product with id: [{}] for user: [{}]", id, userId);

        productService.deleteMyProduct(userId, id);
        
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully", null));
    }
}
