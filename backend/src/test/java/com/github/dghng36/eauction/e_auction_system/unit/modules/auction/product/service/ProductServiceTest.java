package com.github.dghng36.eauction.e_auction_system.unit.modules.auction.product.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;

import com.github.dghng36.eauction.core.base.PageResponse;
import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.core.utils.ConstantsUtils;
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
import com.github.dghng36.eauction.modules.auction.product.service.ProductService;
import com.github.dghng36.eauction.modules.media.dto.internal.MediaFile;
import com.github.dghng36.eauction.modules.media.dto.request.MediaFileUploadRequest;
import com.github.dghng36.eauction.modules.media.service.InternalMediaService;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {
    @Mock private MongoTemplate mongoTemplate;
    @Mock private ProductRepository productRepo;
    @Mock private InternalMediaService internalMediaService;
    @Mock private ProductMapper productMapper;
    @Mock private JobExecutorTasks jobExecutorTasks;

    @InjectMocks private ProductService productService;

    private static final Set<String> ALLOWED_SORT_BY_FIELDS = Set.of(
        "name",
        "createdAt",
        "updatedAt"
    );

    private final String userId = "user-id-123";
    private final String productId = "product-id-456";

    // Use a direct executor so CompletableFuture tasks run synchronously in tests
    private final Executor syncExecutor = Executors.newSingleThreadExecutor();

    private Product mockProduct;
    private ProductResponse mockProductResponse;
    private SearchProductsRequest emptySearchRequest;
    private SearchProductsRequest searchQueryRequest;
    private SearchProductsRequest statusFilterRequest;
    private SearchProductsRequest combinedSearchRequest;
    private CreateProductRequest createProductRequest;
    private MediaFileUploadRequest mediaUploadRequest;

    @BeforeEach
    void setUp() {
        // Stub async executor to run tasks synchronously during tests
        Mockito.lenient()
            .when(jobExecutorTasks.getExecutor()).thenReturn(syncExecutor);
        // Stub runAsync to execute the runnable immediately (synchronously)
        Mockito.lenient()
            .doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(jobExecutorTasks).runAsync(any(Runnable.class));

        mockProduct = Product.builder()
            .id(productId)
            .name("Test Product Name")
            .description("Test product description for unit tests")
            .ownerId(userId)
            .status(ProductStatus.AVAILABLE)
            .isDeleted(false)
            .images(new ArrayList<>(List.of(
                MediaFile.builder()
                    .mediaCode("media-1")
                    .objectKey("object-key-1")
                    .originalUrl("https://example.com/1.jpg")
                    .build()
            )))
            .build();

        mockProductResponse = ProductResponse.builder()
            .id(productId)
            .name(mockProduct.getName())
            .description(mockProduct.getDescription())
            .ownerId(userId)
            .status(ProductStatus.AVAILABLE.name())
            .build();

        emptySearchRequest = SearchProductsRequest.builder().build();

        searchQueryRequest = SearchProductsRequest.builder()
            .searchQuery("Test")
            .build();

        statusFilterRequest = SearchProductsRequest.builder()
            .productStatuses(List.of("AVAILABLE"))
            .build();

        combinedSearchRequest = SearchProductsRequest.builder()
            .searchQuery("Test")
            .productStatuses(List.of("AVAILABLE"))
            .build();

        mediaUploadRequest = MediaFileUploadRequest.builder()
            .mediaCode("media-new")
            .objectKey("object-key-new")
            .build();

        createProductRequest = new CreateProductRequest(
            "New Product Name",
            "New product description for tests",
            List.of(mediaUploadRequest),
            Map.of("color", "red")
        );
    }

    // getProducts

    @Test
    void getProducts_Success_ShouldReturnProductList() {
        int page = 0, size = 10;
        String sortBy = "createdAt", sortDirection = "desc";

        Sort expectedSort = SortUtils.buildSort(sortBy, sortDirection, ALLOWED_SORT_BY_FIELDS);
        PageRequest expectedPageRequest = PageRequest.of(page, size, expectedSort);
        List<Product> products = List.of(mockProduct);

        when(productRepo.findAllByIsDeletedFalse(expectedPageRequest))
            .thenReturn(new PageImpl<>(products, expectedPageRequest, products.size()));
        when(productMapper.toProductResponseList(products)).thenReturn(List.of(mockProductResponse));

        PageResponse<ProductResponse> result = productService.getProducts(page, size, sortBy, sortDirection);

        assertThat(result).isNotNull();
        assertThat(result.getCurrentPage()).isEqualTo(1);
        assertThat(result.getPageSize()).isEqualTo(size);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getData()).containsExactly(mockProductResponse);

        verify(productRepo, times(1)).findAllByIsDeletedFalse(expectedPageRequest);
        verify(productMapper, times(1)).toProductResponseList(products);
    }

    @Test
    void getProducts_NoProducts_ShouldReturnEmptyList() {
        int page = 0, size = 10;
        String sortBy = "createdAt", sortDirection = "desc";

        Sort expectedSort = SortUtils.buildSort(sortBy, sortDirection, ALLOWED_SORT_BY_FIELDS);
        PageRequest expectedPageRequest = PageRequest.of(page, size, expectedSort);

        when(productRepo.findAllByIsDeletedFalse(expectedPageRequest)).thenReturn(Page.empty());
        when(productMapper.toProductResponseList(any())).thenReturn(List.of());

        PageResponse<ProductResponse> result = productService.getProducts(page, size, sortBy, sortDirection);

        assertThat(result).isNotNull();
        assertThat(result.getCurrentPage()).isZero();
        assertThat(result.getData()).isEmpty();

        verify(productRepo, times(1)).findAllByIsDeletedFalse(expectedPageRequest);
    }

    @Test
    void getProducts_DatabaseError_ShouldThrowException() {
        int page = 0, size = 10;
        String sortBy = "createdAt", sortDirection = "desc";

        Sort expectedSort = SortUtils.buildSort(sortBy, sortDirection, ALLOWED_SORT_BY_FIELDS);
        PageRequest expectedPageRequest = PageRequest.of(page, size, expectedSort);

        when(productRepo.findAllByIsDeletedFalse(expectedPageRequest))
            .thenThrow(new RuntimeException("Database error"));

        assertThatThrownBy(() -> productService.getProducts(page, size, sortBy, sortDirection))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Database error");

        verify(productMapper, never()).toProductResponseList(any());
    }

    // getProduct

    @Test
    void getProduct_Success_ShouldReturnProduct() {
        when(productRepo.findByIdAndIsDeletedFalse(productId)).thenReturn(Optional.of(mockProduct));
        when(productMapper.toProductResponse(mockProduct)).thenReturn(mockProductResponse);

        ProductResponse result = productService.getProduct(productId);

        assertThat(result).isEqualTo(mockProductResponse);
        verify(productRepo, times(1)).findByIdAndIsDeletedFalse(productId);
        verify(productMapper, times(1)).toProductResponse(mockProduct);
    }

    @Test
    void getProduct_ProductNotFound_ShouldThrowException() {
        when(productRepo.findByIdAndIsDeletedFalse(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProduct(productId))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("Product not found")
            .extracting(ex -> ((AppException) ex).getStatus())
            .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // searchProducts

    @Test
    void searchProducts_WithSearchQueryRequest_ShouldReturnMatchingProducts() {
        int page = 0, size = 10;
        String sortBy = "createdAt", sortDirection = "desc";
        List<Product> products = List.of(mockProduct);

        when(mongoTemplate.count(any(Query.class), eq(Product.class))).thenReturn(1L);
        when(mongoTemplate.find(any(Query.class), eq(Product.class))).thenReturn(products);
        when(productMapper.toProductResponseList(products)).thenReturn(List.of(mockProductResponse));

        PageResponse<ProductResponse> result = productService.searchProducts(
            searchQueryRequest, page, size, sortBy, sortDirection
        );

        assertThat(result.getCurrentPage()).isEqualTo(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getData()).containsExactly(mockProductResponse);

        verify(mongoTemplate, times(1)).count(any(Query.class), eq(Product.class));
        verify(mongoTemplate, times(1)).find(any(Query.class), eq(Product.class));
    }

    @Test
    void searchProducts_WithSearchQueryRequest_ShouldReturnEmptyList() {
        int page = 0, size = 10;
        String sortBy = "createdAt", sortDirection = "desc";

        when(mongoTemplate.count(any(Query.class), eq(Product.class))).thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), eq(Product.class))).thenReturn(List.of());
        when(productMapper.toProductResponseList(any())).thenReturn(List.of());

        PageResponse<ProductResponse> result = productService.searchProducts(
            searchQueryRequest, page, size, sortBy, sortDirection
        );

        assertThat(result.getCurrentPage()).isZero();
        assertThat(result.getData()).isEmpty();
    }

    @Test
    void searchProducts_WithListStatusFilterRequest_ShouldReturnMatchingProducts() {
        int page = 0, size = 10;
        String sortBy = "createdAt", sortDirection = "desc";
        List<Product> products = List.of(mockProduct);

        when(mongoTemplate.count(any(Query.class), eq(Product.class))).thenReturn(1L);
        when(mongoTemplate.find(any(Query.class), eq(Product.class))).thenReturn(products);
        when(productMapper.toProductResponseList(products)).thenReturn(List.of(mockProductResponse));

        PageResponse<ProductResponse> result = productService.searchProducts(
            statusFilterRequest, page, size, sortBy, sortDirection
        );

        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getStatus()).isEqualTo(ProductStatus.AVAILABLE.name());
    }

    @Test
    void searchProducts_WithListStatusFilterRequest_ShouldReturnEmptyList() {
        int page = 0, size = 10;
        String sortBy = "createdAt", sortDirection = "desc";

        when(mongoTemplate.count(any(Query.class), eq(Product.class))).thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), eq(Product.class))).thenReturn(List.of());
        when(productMapper.toProductResponseList(any())).thenReturn(List.of());

        PageResponse<ProductResponse> result = productService.searchProducts(
            statusFilterRequest, page, size, sortBy, sortDirection
        );

        assertThat(result.getCurrentPage()).isZero();
        assertThat(result.getData()).isEmpty();
    }

    @Test
    void searchProducts_WithInvalidListStatusRequest_ShouldThrowException() {
        SearchProductsRequest invalidStatusRequest = SearchProductsRequest.builder()
            .productStatuses(List.of("INVALID_STATUS"))
            .build();

        assertThatThrownBy(() -> productService.searchProducts(invalidStatusRequest, 0, 10, "createdAt", "desc"))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("Invalid product status")
            .extracting(ex -> ((AppException) ex).getStatus())
            .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(mongoTemplate, never()).count(any(Query.class), eq(Product.class));
    }

    @Test
    void searchProducts_WithSearchQueryAndListStatusRequest_ShouldReturnMatchingProducts() {
        int page = 0, size = 10;
        String sortBy = "createdAt", sortDirection = "desc";
        List<Product> products = List.of(mockProduct);

        when(mongoTemplate.count(any(Query.class), eq(Product.class))).thenReturn(1L);
        when(mongoTemplate.find(any(Query.class), eq(Product.class))).thenReturn(products);
        when(productMapper.toProductResponseList(products)).thenReturn(List.of(mockProductResponse));

        PageResponse<ProductResponse> result = productService.searchProducts(
            combinedSearchRequest, page, size, sortBy, sortDirection
        );

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getData()).containsExactly(mockProductResponse);
    }

    @Test
    void searchProducts_WithSearchQueryAndListStatusRequest_ShouldReturnEmptyList() {
        when(mongoTemplate.count(any(Query.class), eq(Product.class))).thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), eq(Product.class))).thenReturn(List.of());
        when(productMapper.toProductResponseList(any())).thenReturn(List.of());

        PageResponse<ProductResponse> result = productService.searchProducts(
            combinedSearchRequest, 0, 10, "createdAt", "desc"
        );

        assertThat(result.getCurrentPage()).isZero();
        assertThat(result.getData()).isEmpty();
    }

    @Test
    void searchProducts_WithSearchQueryAndInvalidListStatusRequest_ShouldThrowException() {
        SearchProductsRequest invalidCombinedRequest = SearchProductsRequest.builder()
            .searchQuery("Test")
            .productStatuses(List.of("NOT_A_STATUS"))
            .build();

        assertThatThrownBy(() -> productService.searchProducts(invalidCombinedRequest, 0, 10, "createdAt", "desc"))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("Invalid product status");
    }

    @Test
    void searchProducts_WithSearchQueryAndSortByRequest_ShouldReturnMatchingProductsAndSortThem() {
        int page = 0, size = 10;
        String sortBy = "name", sortDirection = "asc";
        List<Product> products = List.of(mockProduct);

        when(mongoTemplate.count(any(Query.class), eq(Product.class))).thenReturn(1L);
        when(mongoTemplate.find(any(Query.class), eq(Product.class))).thenReturn(products);
        when(productMapper.toProductResponseList(products)).thenReturn(List.of(mockProductResponse));

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);

        PageResponse<ProductResponse> result = productService.searchProducts(
            searchQueryRequest, page, size, sortBy, sortDirection
        );

        assertThat(result.getData()).containsExactly(mockProductResponse);

        verify(mongoTemplate).find(queryCaptor.capture(), eq(Product.class));
        Query capturedQuery = queryCaptor.getValue();
        assertThat(capturedQuery.getLimit()).isEqualTo(size);
        assertThat(capturedQuery.getSkip()).isZero();
    }

    // createMyProduct

    @Test
    void createMyProduct_Success_ShouldReturnCreatedProduct() {
        MediaFile resolvedMedia = MediaFile.builder()
            .mediaCode(mediaUploadRequest.getMediaCode())
            .objectKey(mediaUploadRequest.getObjectKey())
            .originalUrl("https://example.com/new.jpg")
            .build();

        when(productRepo.existsByOwnerIdAndNameAndIsDeletedFalse(userId, createProductRequest.getName())).thenReturn(false);
        when(internalMediaService.resolve(mediaUploadRequest.getMediaCode()))
            .thenReturn(resolvedMedia.getOriginalUrl());
        when(productMapper.toProductEntity(eq(userId), eq(createProductRequest), any()))
            .thenReturn(mockProduct);
        when(productRepo.save(mockProduct)).thenReturn(mockProduct);
        when(productMapper.toProductResponse(mockProduct)).thenReturn(mockProductResponse);

        ProductResponse result = productService.createMyProduct(userId, createProductRequest);

        assertThat(result).isEqualTo(mockProductResponse);
        verify(productRepo, times(1)).existsByOwnerIdAndNameAndIsDeletedFalse(userId, createProductRequest.getName());
        verify(productRepo, times(1)).save(mockProduct);
    }

    @Test
    void createMyProduct_InvalidInput_ShouldThrowException() {
        when(productRepo.existsByOwnerIdAndNameAndIsDeletedFalse(userId, createProductRequest.getName())).thenReturn(true);

        assertThatThrownBy(() -> productService.createMyProduct(userId, createProductRequest))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("Product has already exists")
            .extracting(ex -> ((AppException) ex).getStatus())
            .isEqualTo(HttpStatus.CONFLICT);

        verify(productRepo, never()).save(any());
    }

    // getMyProducts

    @Test
    void getMyProducts_Success_ShouldReturnProductList() {
        int page = 0, size = 10;
        String sortBy = "createdAt", sortDirection = "desc";

        Sort expectedSort = SortUtils.buildSort(sortBy, sortDirection, ALLOWED_SORT_BY_FIELDS);
        PageRequest expectedPageRequest = PageRequest.of(page, size, expectedSort);
        List<Product> products = List.of(mockProduct);

        when(productRepo.findAllByOwnerIdAndIsDeletedFalse(userId, expectedPageRequest))
            .thenReturn(new PageImpl<>(products, expectedPageRequest, products.size()));
        when(productMapper.toProductResponseList(products)).thenReturn(List.of(mockProductResponse));

        PageResponse<ProductResponse> result = productService.getMyProducts(userId, page, size, sortBy, sortDirection);

        assertThat(result.getCurrentPage()).isEqualTo(1);
        assertThat(result.getData()).containsExactly(mockProductResponse);
        verify(productRepo, times(1)).findAllByOwnerIdAndIsDeletedFalse(userId, expectedPageRequest);
    }

    @Test
    void getMyProducts_NoProducts_ShouldReturnEmptyList() {
        Sort expectedSort = SortUtils.buildSort("createdAt", "desc", ALLOWED_SORT_BY_FIELDS);
        PageRequest expectedPageRequest = PageRequest.of(0, 10, expectedSort);

        when(productRepo.findAllByOwnerIdAndIsDeletedFalse(userId, expectedPageRequest)).thenReturn(Page.empty());
        when(productMapper.toProductResponseList(any())).thenReturn(List.of());

        PageResponse<ProductResponse> result = productService.getMyProducts(userId, 0, 10, "createdAt", "desc");

        assertThat(result.getCurrentPage()).isZero();
        assertThat(result.getData()).isEmpty();
    }

    // searchMyProducts

    @Test
    void searchMyProducts_WithSearchQueryRequest_ShouldReturnMatchingProducts() {
        List<Product> products = List.of(mockProduct);

        when(mongoTemplate.count(any(Query.class), eq(Product.class))).thenReturn(1L);
        when(mongoTemplate.find(any(Query.class), eq(Product.class))).thenReturn(products);
        when(productMapper.toProductResponseList(products)).thenReturn(List.of(mockProductResponse));

        PageResponse<ProductResponse> result = productService.searchMyProducts(
            userId, searchQueryRequest, 0, 10, "createdAt", "desc"
        );

        assertThat(result.getData()).containsExactly(mockProductResponse);
    }

    @Test
    void searchMyProducts_WithSearchQueryRequest_ShouldReturnEmptyList() {
        when(mongoTemplate.count(any(Query.class), eq(Product.class))).thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), eq(Product.class))).thenReturn(List.of());
        when(productMapper.toProductResponseList(any())).thenReturn(List.of());

        PageResponse<ProductResponse> result = productService.searchMyProducts(
            userId, searchQueryRequest, 0, 10, "createdAt", "desc"
        );

        assertThat(result.getCurrentPage()).isZero();
        assertThat(result.getData()).isEmpty();
    }

    @Test
    void searchMyProducts_WithListStatusFilterRequest_ShouldReturnMatchingProducts() {
        List<Product> products = List.of(mockProduct);

        when(mongoTemplate.count(any(Query.class), eq(Product.class))).thenReturn(1L);
        when(mongoTemplate.find(any(Query.class), eq(Product.class))).thenReturn(products);
        when(productMapper.toProductResponseList(products)).thenReturn(List.of(mockProductResponse));

        PageResponse<ProductResponse> result = productService.searchMyProducts(
            userId, statusFilterRequest, 0, 10, "createdAt", "desc"
        );

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getData()).hasSize(1);
    }

    @Test
    void searchMyProducts_WithListStatusFilterRequest_ShouldReturnEmptyList() {
        when(mongoTemplate.count(any(Query.class), eq(Product.class))).thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), eq(Product.class))).thenReturn(List.of());
        when(productMapper.toProductResponseList(any())).thenReturn(List.of());

        PageResponse<ProductResponse> result = productService.searchMyProducts(
            userId, statusFilterRequest, 0, 10, "createdAt", "desc"
        );

        assertThat(result.getData()).isEmpty();
    }

    // getMyProduct

    @Test
    void getMyProduct_Success_ShouldReturnProduct() {
        when(productRepo.findByIdAndIsDeletedFalse(productId)).thenReturn(Optional.of(mockProduct));
        when(productMapper.toProductResponse(mockProduct)).thenReturn(mockProductResponse);

        ProductResponse result = productService.getMyProduct(userId, productId);

        assertThat(result).isEqualTo(mockProductResponse);
    }

    @Test
    void getMyProduct_ProductNotFound_ShouldThrowException() {
        when(productRepo.findByIdAndIsDeletedFalse(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getMyProduct(userId, productId))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("Product not found");
    }

    @Test
    void getMyProduct_UnauthorizedAccess_ShouldThrowException() {
        Product otherOwnerProduct = Product.builder()
            .id(productId)
            .ownerId("other-user-id")
            .name(mockProduct.getName())
            .build();

        when(productRepo.findByIdAndIsDeletedFalse(productId)).thenReturn(Optional.of(otherOwnerProduct));

        assertThatThrownBy(() -> productService.getMyProduct(userId, productId))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("Unauthorized access")
            .extracting(ex -> ((AppException) ex).getStatus())
            .isEqualTo(HttpStatus.FORBIDDEN);
    }

    // updateMyProduct

    @Test
    void updateMyProduct_Success_ShouldReturnUpdatedProduct() {
        UpdateMyProductRequest updateRequest = UpdateMyProductRequest.builder()
            .name("Updated Product Name")
            .description("Updated product description text")
            .build();

        Product updatedProduct = Product.builder()
            .id(productId)
            .name(updateRequest.getName())
            .description(updateRequest.getDescription())
            .ownerId(userId)
            .status(ProductStatus.AVAILABLE)
            .build();

        ProductResponse updatedResponse = ProductResponse.builder()
            .id(productId)
            .name(updateRequest.getName())
            .description(updateRequest.getDescription())
            .ownerId(userId)
            .status(ProductStatus.AVAILABLE.name())
            .build();

        when(productRepo.findByIdAndIsDeletedFalse(productId)).thenReturn(Optional.of(mockProduct));
        when(productRepo.save(mockProduct)).thenReturn(updatedProduct);
        when(productMapper.toProductResponse(updatedProduct)).thenReturn(updatedResponse);

        ProductResponse result = productService.updateMyProduct(userId, productId, updateRequest);

        assertThat(result.getName()).isEqualTo(updateRequest.getName());
        verify(productMapper, times(1)).updateProductEntity(mockProduct, updateRequest);
        verify(productRepo, times(1)).save(mockProduct);
    }

    @Test
    void updateMyProduct_ProductNotFound_ShouldThrowException() {
        when(productRepo.findByIdAndIsDeletedFalse(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateMyProduct(
            userId, productId, UpdateMyProductRequest.builder().name("New Name").build()
        ))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("Product not found");
    }

    @Test
    void updateMyProduct_UnauthorizedAccess_ShouldThrowForbidden() {
        Product otherOwnerProduct = Product.builder()
            .id(productId)
            .ownerId("other-user-id")
            .name(mockProduct.getName())
            .build();

        when(productRepo.findByIdAndIsDeletedFalse(productId)).thenReturn(Optional.of(otherOwnerProduct));

        assertThatThrownBy(() -> productService.updateMyProduct(
            userId, productId, UpdateMyProductRequest.builder().name("New Name").build()
        ))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("Unauthorized access")
            .extracting(ex -> ((AppException) ex).getStatus())
            .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void updateMyProduct_WithMetadata_ShouldUpdateMetadataByFIFOAlgorithm() {
        Map<String, Object> existingMetadata = new LinkedHashMap<>();
        for (int i = 0; i < ConstantsUtils.MetadataConstants.MAX_METADATA_SIZE; i++) {
            existingMetadata.put("key" + i, "value" + i);
        }

        Product productWithMetadata = Product.builder()
            .id(productId)
            .ownerId(userId)
            .name(mockProduct.getName())
            .description(mockProduct.getDescription())
            .metadata(existingMetadata)
            .status(ProductStatus.AVAILABLE)
            .build();

        UpdateMyProductRequest updateRequest = UpdateMyProductRequest.builder()
            .metadata(Map.of("newKey", "newValue"))
            .build();

        when(productRepo.findByIdAndIsDeletedFalse(productId)).thenReturn(Optional.of(productWithMetadata));
        when(productRepo.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productMapper.toProductResponse(any(Product.class))).thenReturn(mockProductResponse);

        productService.updateMyProduct(userId, productId, updateRequest);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepo).save(productCaptor.capture());

        Map<String, Object> savedMetadata = productCaptor.getValue().getMetadata();
        assertThat(savedMetadata).hasSize(ConstantsUtils.MetadataConstants.MAX_METADATA_SIZE);
        assertThat(savedMetadata).doesNotContainKey("key0");
        assertThat(savedMetadata).containsEntry("newKey", "newValue");
    }

    @Test
    void updateMyProduct_WithMedia_ShouldUpdateMediaByFIFOAlgorithm() {
        List<MediaFile> fullImages = new ArrayList<>();
        for (int i = 0; i < ConstantsUtils.MediaFileConstants.MAX_MEDIA_FILE_URL; i++) {
            fullImages.add(MediaFile.builder()
                .mediaCode("media-" + i)
                .objectKey("object-key-" + i)
                .originalUrl("https://example.com/" + i + ".jpg")
                .build());
        }

        Product productWithImages = Product.builder()
            .id(productId)
            .ownerId(userId)
            .name(mockProduct.getName())
            .description(mockProduct.getDescription())
            .images(fullImages)
            .status(ProductStatus.AVAILABLE)
            .build();

        UpdateMyProductRequest updateRequest = UpdateMyProductRequest.builder()
            .images(List.of(mediaUploadRequest))
            .build();

        when(productRepo.findByIdAndIsDeletedFalse(productId)).thenReturn(Optional.of(productWithImages));
        when(internalMediaService.resolve(mediaUploadRequest.getMediaCode()))
            .thenReturn("https://example.com/new.jpg");
        when(productRepo.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productMapper.toProductResponse(any(Product.class))).thenReturn(mockProductResponse);

        productService.updateMyProduct(userId, productId, updateRequest);

        verify(internalMediaService, times(1)).delete("object-key-0");

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepo).save(productCaptor.capture());

        List<MediaFile> savedImages = productCaptor.getValue().getImages();
        assertThat(savedImages).hasSize(ConstantsUtils.MediaFileConstants.MAX_MEDIA_FILE_URL);
        assertThat(savedImages.get(savedImages.size() - 1).getMediaCode()).isEqualTo(mediaUploadRequest.getMediaCode());
    }

    // deleteMyProduct

    @Test
    void deleteMyProduct_Success_ShouldDeleteProduct() {
        when(productRepo.findByIdAndIsDeletedFalse(productId)).thenReturn(Optional.of(mockProduct));
        doNothing().when(internalMediaService).delete(anyString());
        when(productRepo.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        productService.deleteMyProduct(userId, productId);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepo).save(productCaptor.capture());

        Product deletedProduct = productCaptor.getValue();
        assertThat(deletedProduct.getIsDeleted()).isTrue();
        assertThat(deletedProduct.getDeletedAt()).isNotNull();
        assertThat(deletedProduct.getImages()).isNull();
        verify(internalMediaService, times(1)).delete("object-key-1");
    }

    @Test
    void deleteMyProduct_ProductNotFound_ShouldThrowException() {
        when(productRepo.findByIdAndIsDeletedFalse(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteMyProduct(userId, productId))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("Product not found");

        verify(productRepo, never()).save(any());
    }

    @Test
    void deleteMyProduct_UnauthorizedAccess_ShouldThrowException() {
        Product otherOwnerProduct = Product.builder()
            .id(productId)
            .ownerId("other-user-id")
            .name(mockProduct.getName())
            .images(mockProduct.getImages())
            .build();

        when(productRepo.findByIdAndIsDeletedFalse(productId)).thenReturn(Optional.of(otherOwnerProduct));

        assertThatThrownBy(() -> productService.deleteMyProduct(userId, productId))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("Unauthorized access")
            .extracting(ex -> ((AppException) ex).getStatus())
            .isEqualTo(HttpStatus.FORBIDDEN);

        verify(internalMediaService, never()).delete(anyString());
        verify(productRepo, never()).save(any());
    }

    // updateProductStatus

    @Test
    void updateProductStatus_Success_ShouldUpdateStatus() {
        UpdateProductStatusRequest statusRequest = UpdateProductStatusRequest.builder()
            .newProductStatus("IN_AUCTION")
            .build();

        Product updatedProduct = Product.builder()
            .id(productId)
            .name(mockProduct.getName())
            .description(mockProduct.getDescription())
            .ownerId(userId)
            .status(ProductStatus.IN_AUCTION)
            .build();

        ProductResponse updatedResponse = ProductResponse.builder()
            .id(productId)
            .status(ProductStatus.IN_AUCTION.name())
            .build();

        when(productRepo.findByIdAndIsDeletedFalse(productId)).thenReturn(Optional.of(mockProduct));
        when(productRepo.save(mockProduct)).thenReturn(updatedProduct);
        when(productMapper.toProductResponse(updatedProduct)).thenReturn(updatedResponse);

        ProductResponse result = productService.updateProductStatus(productId, statusRequest);

        assertThat(result.getStatus()).isEqualTo(ProductStatus.IN_AUCTION.name());
        verify(productRepo, times(1)).save(mockProduct);
    }

    @Test
    void updateProductStatus_ProductNotFound_ShouldThrowException() {
        when(productRepo.findByIdAndIsDeletedFalse(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProductStatus(
            productId, UpdateProductStatusRequest.builder().newProductStatus("SOLD").build()
        ))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("Product not found");
    }
}