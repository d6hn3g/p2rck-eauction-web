package com.github.dghng36.eauction.modules.auction.product.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.github.dghng36.eauction.core.base.PageResponse;
import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.core.utils.ConstantsUtils;
import com.github.dghng36.eauction.core.utils.MetadataUtils;
import com.github.dghng36.eauction.core.utils.SortUtils;
import com.github.dghng36.eauction.infra.config.async.JobExecutorTasks;
import com.github.dghng36.eauction.modules.auction.enums.ProductStatus;
import com.github.dghng36.eauction.modules.auction.product.dto.request.CreateProductRequest;
import com.github.dghng36.eauction.modules.auction.product.dto.request.SearchProductsRequest;
import com.github.dghng36.eauction.modules.auction.product.dto.request.UpdateMyProductRequest;
import com.github.dghng36.eauction.modules.auction.product.dto.request.UpdateProductStatusRequest;
import com.github.dghng36.eauction.modules.auction.product.dto.response.ProductResponse;
import com.github.dghng36.eauction.modules.auction.product.mapper.ProductMapper;
import com.github.dghng36.eauction.modules.auction.product.model.Product;
import com.github.dghng36.eauction.modules.auction.product.repository.ProductRepository;
import com.github.dghng36.eauction.modules.media.dto.internal.MediaFile;
import com.github.dghng36.eauction.modules.media.service.InternalMediaService;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ProductService {
    MongoTemplate mongoTemplate;
    ProductRepository productRepo;

    InternalMediaService iMediaService;

    ProductMapper productMapper;

    JobExecutorTasks jobExecutorTasks;

    static Set<String> ALLOWED_SORT_BY_FIELDS = Set.of(
        "name",
        "createdAt",
        "updatedAt"
    );

    // Public product methods
    public PageResponse<ProductResponse> getProducts(int page, int size, String sortBy, String sortDirection) {
       // Build sort object
        Sort sortBuilt = SortUtils.buildSort(sortBy, sortDirection, ALLOWED_SORT_BY_FIELDS);

        Page<Product> productPage = productRepo.findAllByIsDeletedFalse(PageRequest.of(page, size, sortBuilt));

        // Map products to ProductResponse DTOs
        List<ProductResponse> productResponses = productMapper.toProductResponseList(productPage.getContent());

        return PageResponse.<ProductResponse>builder()
            .currentPage(productPage.getTotalElements() == 0 ? 0 : page + 1)
            .pageSize(size)
            .totalPages(productPage.getTotalPages())
            .totalElements(productPage.getTotalElements())
            .data(productResponses)
            .build();
    }

    public ProductResponse getProduct(String id) {
        // Find the product by ID
        Product product = productRepo.findByIdAndIsDeletedFalse(id)
            .orElseThrow(() -> new AppException("Product not found", HttpStatus.NOT_FOUND));

        return productMapper.toProductResponse(product);
    }

    public PageResponse<ProductResponse> searchProducts(SearchProductsRequest searchProductsRequest, int page, int size, String sortBy, String sortDirection) {
        // Create new query and criteria list
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        applyCriteria(criteriaList, searchProductsRequest);

        return applyExecuteQuery(query, criteriaList, page, size, sortBy, sortDirection);
    }


    // My product methods
    @Transactional
    public ProductResponse createMyProduct(String userId, CreateProductRequest createProductRequest) {
        // Check if the product already exists
        if (productRepo.existsByOwnerIdAndNameAndIsDeletedFalse(userId, createProductRequest.getName())) {
            log.warn("Attempted to create a product with an existing name: [{}] by user: [{}]", 
                createProductRequest.getName(), userId
            );

            throw new AppException("Product has already exists", HttpStatus.CONFLICT);
        }

        // Get original url from media service and set to images
        List<MediaFile> images = createProductRequest.getImages() != null ?
            createProductRequest.getImages().stream()
                .map(image -> {
                    String originalUrl = iMediaService.resolve(image.getMediaCode());
                    return MediaFile.builder()
                        .mediaCode(image.getMediaCode())
                        .objectKey(image.getObjectKey())
                        .originalUrl(originalUrl)
                        .build();
                })
                .toList() : new ArrayList<>(List.of());

        Product newProduct = productMapper.toProductEntity(userId, createProductRequest, images);

        // Save the product to the database
        Product savedProduct = productRepo.save(newProduct);

        log.info("Created new product with ID: [{}] for user: [{}]", savedProduct.getId(), userId);

        // Map the saved product to a ProductResponse DTO and return it
        return productMapper.toProductResponse(savedProduct);
    }

    public PageResponse<ProductResponse> getMyProducts(String userId, int page, int size, String sortBy, String sortDirection) {
        // Build sort object
        Sort sortBuilt = SortUtils.buildSort(sortBy, sortDirection, ALLOWED_SORT_BY_FIELDS);

        Page<Product> productPage = productRepo.findAllByOwnerIdAndIsDeletedFalse(userId, PageRequest.of(page, size, sortBuilt));

        List<ProductResponse> productResponses = productMapper.toProductResponseList(productPage.getContent());

        return PageResponse.<ProductResponse>builder()
            .currentPage(productPage.getTotalElements() == 0 ? 0 : page + 1)
            .pageSize(size)
            .totalPages(productPage.getTotalPages())
            .totalElements(productPage.getTotalElements())
            .data(productResponses)
            .build();
    }

    public ProductResponse getMyProduct(String userId, String productId) {
        // Check if the product belongs to the user
        Product product = productRepo.findByIdAndIsDeletedFalse(productId)
            .orElseThrow(() -> new AppException("Product not found", HttpStatus.NOT_FOUND));

        // Check if the product belongs to the user
        if (!product.getOwnerId().equals(userId)) {
            throw new AppException("Unauthorized access", HttpStatus.FORBIDDEN);
        }

        return productMapper.toProductResponse(product);
    }

    public PageResponse<ProductResponse> searchMyProducts(String userId, SearchProductsRequest searchMyProductsRequest, int page, int size, String sortBy, String sortDirection) {
        // Create new query and criteria list
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        applyCriteria(criteriaList, searchMyProductsRequest);

        return applyExecuteQuery(
            query, criteriaList, 
            page, size, 
            sortBy, sortDirection,
            true, userId);
    }

    @Transactional
    public ProductResponse updateMyProduct(String userId, String productId, UpdateMyProductRequest updateMyProductRequest) {
        // Find the product by ID
        Product product = productRepo.findByIdAndIsDeletedFalse(productId)
            .orElseThrow(() -> new AppException("Product not found", HttpStatus.NOT_FOUND));

        // Check if the product belongs to the user
        if (!product.getOwnerId().equals(userId)) {
            log.warn("Unauthorized attempt to update product ID: [{}] by user:[{}]", productId, userId);

            throw new AppException("Unauthorized access", HttpStatus.FORBIDDEN);
        }

        // Update product fields
        productMapper.updateProductEntity(product, updateMyProductRequest);
        
        // Handle images and metadata with FIFO logic
        /*
         * Handle images with FIFO logic
         * If the new images exceed the max limit, remove the oldest images first.
         */
        if (updateMyProductRequest.getImages() != null && !updateMyProductRequest.getImages().isEmpty()) {
            List<MediaFile> currentImages = product.getImages() != null ? new ArrayList<>(product.getImages()) : new ArrayList<>();
            
            List<CompletableFuture<MediaFile>> imageFutures = updateMyProductRequest.getImages().stream()
                .map(image -> CompletableFuture.supplyAsync(() -> {
                    String originalUrl = iMediaService.resolve(image.getMediaCode());
                    return MediaFile.builder()
                        .mediaCode(image.getMediaCode())
                        .objectKey(image.getObjectKey())
                        .originalUrl(originalUrl)
                        .build();
                }, jobExecutorTasks.getExecutor()))
                .toList();

            CompletableFuture.allOf(imageFutures.toArray(CompletableFuture[]::new)).join();

            List<MediaFile> resolvedImages = imageFutures.stream()
                .map(future -> future.join())
                .toList();

            List<String> keysToDelete = new ArrayList<>();

            for (MediaFile newImage : resolvedImages) {
                if (currentImages.size() >= ConstantsUtils.MediaFileConstants.MAX_MEDIA_FILE_URL) {
                    MediaFile imageToRemove = currentImages.remove(0);
                    if (imageToRemove.getObjectKey() != null) {
                        keysToDelete.add(imageToRemove.getObjectKey());
                    }
                }
                currentImages.add(newImage);
            }

            product.setImages(currentImages);

            if (!keysToDelete.isEmpty()) {
                jobExecutorTasks.runAsync(() -> {
                    for (String key : keysToDelete) {
                        try {
                            iMediaService.delete(key);
                        } catch (Exception ex) {
                            log.error("Failed to delete old image cloud storage object key: [{}]", key, ex);
                        }
                    }
                });
            }
        }

        /*
         * Handle metadata with FIFO logic
         * If the new metadata key already exists, 
         * it will be updated and moved to the end of the map to maintain the insertion order.
         */
        if (updateMyProductRequest.getMetadata() != null && !updateMyProductRequest.getMetadata().isEmpty()) {
            Map<String, Object> currentMetadata = product.getMetadata() != null ? new LinkedHashMap<>(product.getMetadata()) : new LinkedHashMap<>();
            Map<String, Object> newMetadata = MetadataUtils.sanitizeDynamicMetadata(updateMyProductRequest.getMetadata());

            long newKeysCount = newMetadata.keySet().stream()
                .filter(key -> !currentMetadata.containsKey(key))
                .count();

            long overflowCount = currentMetadata.size() + newKeysCount - ConstantsUtils.MetadataConstants.MAX_METADATA_SIZE;

            while (overflowCount > 0 && !currentMetadata.isEmpty()) {
                String keyToRemove = currentMetadata.keySet().iterator().next();
                currentMetadata.remove(keyToRemove);
                overflowCount--;
            }

            newMetadata.forEach((key, value) -> {
                currentMetadata.remove(key);
                currentMetadata.put(key, value);
            });

            product.setMetadata(currentMetadata);
        }

        // Save the updated product to the database
        Product updatedProduct = productRepo.save(product);

        log.info("Updated product ID: [{}] for user: [{}]", updatedProduct.getId(), userId);

        // Map the updated product to a ProductResponse DTO and return it
        return productMapper.toProductResponse(updatedProduct);
    }
    
    @Transactional
    public void deleteMyProduct(String userId, String productId) {
        // Find the product by ID
        Product product = productRepo.findByIdAndIsDeletedFalse(productId)
            .orElseThrow(() -> new AppException("Product not found", HttpStatus.NOT_FOUND));

        // Check if the product belongs to the user
        if (!product.getOwnerId().equals(userId)) {
            log.warn("Unauthorized attempt to delete product ID: [{}] by user: [{}]", productId, userId);

            throw new AppException("Unauthorized access", HttpStatus.FORBIDDEN);
        }

        // Delete associated media
        if (product.getImages() != null) {
            for (MediaFile image : product.getImages()) {
                iMediaService.delete(image.getObjectKey());
            }

            product.setImages(null);
        }

        // Delete the product from the database
        product.setIsDeleted(true);
        product.setDeletedAt(LocalDateTime.now());

        log.info("Deleted product ID: [{}] for user ID: [{}]", product.getId(), userId);

        productRepo.save(product);
    }

    // Manager and admin product methods
    @Transactional
    public ProductResponse updateProductStatus(String productId, UpdateProductStatusRequest updateProductStatusRequest) {
        // Find the product by ID
        Product product = productRepo.findByIdAndIsDeletedFalse(productId)
            .orElseThrow(() -> new AppException("Product not found", HttpStatus.NOT_FOUND));

        // Update product status
        ProductStatus newProductStatus = ProductStatus.fromString(updateProductStatusRequest.getNewProductStatus())
            .orElse(product.getStatus());

        product.setStatus(newProductStatus);
        
        // Save the updated product to the database
        Product updatedProduct = productRepo.save(product);

        log.info("Updated product status for product ID: [{}] to status: [{}]", updatedProduct.getId(), newProductStatus);

        // Map the updated product to a ProductResponse DTO and return it
        return productMapper.toProductResponse(updatedProduct);
    }

    // Utility methods
    /*
     * These methods are used for search product
     */
    private void applyCriteria(List<Criteria> criteriaList, SearchProductsRequest searchProductsRequest) {
        criteriaList.add(Criteria.where("isDeleted").is(false));

        // Add search query criteria for name and description
        if (StringUtils.hasText(searchProductsRequest.getSearchQuery())) {
            String regex = Pattern.quote(searchProductsRequest.getSearchQuery().trim());

            criteriaList.add(new Criteria().orOperator(
                Criteria.where("name").regex(regex, "i"),
                Criteria.where("description").regex(regex, "i")
            ));
        }

        // Add product status criteria
        if (searchProductsRequest.getProductStatuses() != null && !searchProductsRequest.getProductStatuses().isEmpty()) {
            List<ProductStatus> statusList = searchProductsRequest.getProductStatuses().stream()
                .filter(StringUtils::hasText)
                .map(status -> ProductStatus.fromString(status)
                    .orElseThrow(() -> new AppException("Invalid product status: " + status, HttpStatus.BAD_REQUEST))
                )
                .toList();
            
            if (!statusList.isEmpty()) {
                criteriaList.add(Criteria.where("status").in(statusList));   
            }
        }
    }

    private PageResponse<ProductResponse> applyExecuteQuery(
        Query baseQuery, List<Criteria> criteriaList, 
        int page, int size, 
        String sortBy, String sortDirection,
        Object... options
    ) {
        /*
         * Check if the query is for searching my products,
         * if options[0] is true then filter products that belong to the user, options[1] is 
         * the userId to filter products
         * if options[0] is false then return all products without filtering by user
         */
        boolean isSearchMyProducts = (
            options != null && options.length > 0 && options[0] instanceof Boolean 
            && (Boolean) options[0]
        );
        String currentUserId = (
            isSearchMyProducts && options != null && options.length > 1 && options[1] instanceof String 
                ? (String) options[1] 
                : null
        );

        if (isSearchMyProducts && currentUserId != null) {
            criteriaList.add(Criteria.where("ownerId").is(currentUserId));
        }

        // Combine criteria with AND operator
        if (!criteriaList.isEmpty()) {
            criteriaList.forEach(baseQuery::addCriteria);
        }

        // Count total elements for pagination
        long totalElements = mongoTemplate.count(baseQuery, Product.class);
        
        // Build sort object
        Sort sortBuilt = SortUtils.buildSort(sortBy, sortDirection, ALLOWED_SORT_BY_FIELDS);
        
        baseQuery.with(PageRequest.of(page, size, sortBuilt));

        // Execute query
        List<Product> productPage = mongoTemplate.find(baseQuery, Product.class);

        List<ProductResponse> productResponses = productMapper.toProductResponseList(productPage);

        return PageResponse.<ProductResponse>builder()
            .currentPage(totalElements == 0 ? 0 : page + 1)
            .pageSize(size)
            .totalPages((int) Math.ceil((double) totalElements / size))
            .totalElements(totalElements)
            .data(productResponses)
            .build();
    }
}
