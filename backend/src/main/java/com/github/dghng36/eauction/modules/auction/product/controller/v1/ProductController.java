package com.github.dghng36.eauction.modules.auction.product.controller.v1;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.dghng36.eauction.core.base.ApiResponse;
import com.github.dghng36.eauction.core.base.PageResponse;
import com.github.dghng36.eauction.modules.auction.product.dto.request.SearchProductsRequest;
import com.github.dghng36.eauction.modules.auction.product.dto.response.ProductResponse;
import com.github.dghng36.eauction.modules.auction.product.service.ProductService;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductController {
    ProductService productService;

    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    @GetMapping
    @Validated
    ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getProducts(
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
        PageResponse<ProductResponse> productsPage = productService.getProducts(
            page, size, 
            sortBy, sortDirection
        );
        
        return ResponseEntity.ok(ApiResponse.success("Get all products successfully", productsPage));
    }

    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    @GetMapping("/{id}")
    ResponseEntity<ApiResponse<ProductResponse>> getProduct(@PathVariable String id) {
        ProductResponse product = productService.getProduct(id);
        
        return ResponseEntity.ok(ApiResponse.success("Get product successfully", product));
    }

    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    @PostMapping("/search")
    @Validated
    ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> searchProducts(
        @RequestBody SearchProductsRequest  searchProductsRequest,
        @RequestParam(defaultValue = "0") 
        @Min(value = 0, message = "Page index must be at least 0") 
        int page,

        @RequestParam(defaultValue = "10") 
        @Min(value = 1, message = "Page size must not be less than 1")
        @Max(value = 50, message = "Page size must not be greater than 50")
        int size,

        @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
        @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        PageResponse<ProductResponse> productsPage = productService.searchProducts(
            searchProductsRequest, 
            page, size,
            sortBy, sortDirection
        );
        
        return ResponseEntity.ok(ApiResponse.success("Search products successfully", productsPage));
    }
    
}