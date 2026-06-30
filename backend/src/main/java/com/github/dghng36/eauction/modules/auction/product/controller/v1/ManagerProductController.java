package com.github.dghng36.eauction.modules.auction.product.controller.v1;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.dghng36.eauction.core.base.ApiResponse;
import com.github.dghng36.eauction.modules.auction.product.dto.request.UpdateProductStatusRequest;
import com.github.dghng36.eauction.modules.auction.product.dto.response.ProductResponse;
import com.github.dghng36.eauction.modules.auction.product.service.ProductService;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/manager/products/management")
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ManagerProductController {
    ProductService productService;

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PatchMapping("/{id}")
    ResponseEntity<ApiResponse<ProductResponse>> updateProductStatus(
        @PathVariable String id, 
        @RequestBody UpdateProductStatusRequest updateProductStatusRequest
    ) {
        log.info("Received request to update product status for product ID: [{}]", id);
        
        ProductResponse updatedProduct = productService.updateProductStatus(id, updateProductStatusRequest);
        
        return ResponseEntity.ok(ApiResponse.success("Product status updated successfully", updatedProduct));
    }
}
