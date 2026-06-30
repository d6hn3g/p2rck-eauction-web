package com.github.dghng36.eauction.modules.finance.payment.controller.v1;

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
import com.github.dghng36.eauction.infra.config.security.annotation.AuthInfo;
import com.github.dghng36.eauction.infra.config.security.annotation.AuthInfoType;
import com.github.dghng36.eauction.modules.finance.payment.dto.request.SearchPaymentRequest;
import com.github.dghng36.eauction.modules.finance.payment.dto.response.PaymentResponse;
import com.github.dghng36.eauction.modules.finance.payment.dto.response.PaymentStatusResponse;
import com.github.dghng36.eauction.modules.finance.payment.service.PaymentService;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;



@RestController
@RequestMapping("/api/v1/users/me/payments")
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class MyPaymentController {
    PaymentService paymentService;

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @GetMapping
    @Validated
    ResponseEntity<ApiResponse<PageResponse<PaymentResponse>>> getMyPayments(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @RequestParam(defaultValue = "0") 
        @Min(value = 0, message = "Page index must not be less than 0")
        int page,

        @RequestParam(defaultValue = "10") 
        @Min(value = 1, message = "Page size must be at least 1")
        @Max(value = 50, message = "Page size must not be greater than 50")
        int size,
        
        @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
        @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        PageResponse<PaymentResponse> paymentResponses = paymentService.getMyPayments(
            userId,
            page, size,
            sortBy, sortDirection
        );

        return ResponseEntity.ok(ApiResponse.success("Get my payments successfully", paymentResponses));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @PostMapping("/search")
    @Validated
    ResponseEntity<ApiResponse<PageResponse<PaymentResponse>>> searchMyPayments(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @RequestBody SearchPaymentRequest searchPaymentRequest,
        @RequestParam(defaultValue = "0") 
        @Min(value = 0, message = "Page index must not be less than 0")
        int page,

        @RequestParam(defaultValue = "10") 
        @Min(value = 1, message = "Page size must be at least 1")
        @Max(value = 50, message = "Page size must not be greater than 50")
        int size,

        @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
        @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        PageResponse<PaymentResponse> paymentResponses = paymentService.searchMyPayments(
            userId,
            searchPaymentRequest,
            page, size,
            sortBy, sortDirection
        );

        return ResponseEntity.ok(ApiResponse.success("Search my payments successfully", paymentResponses));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @GetMapping("/{paymentId}")
    ResponseEntity<ApiResponse<PaymentResponse>> getMyPayment(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @PathVariable String paymentId
    ) {
        PaymentResponse paymentResponse = paymentService.getUserPayment(
            userId,
            paymentId
        );

        return ResponseEntity.ok(ApiResponse.success("Get my payment successfully", paymentResponse));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @GetMapping("/{paymentId}/status")
    ResponseEntity<ApiResponse<PaymentStatusResponse>> getMyPaymentStatus(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @PathVariable String paymentId
    ) {
        PaymentStatusResponse paymentStatusResponse = paymentService.getUserPaymentStatus(
            userId,
            paymentId
        );

        return ResponseEntity.ok(ApiResponse.success("Get payment status successfully", paymentStatusResponse));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @PostMapping("/{paymentId}/cancel")
    ResponseEntity<ApiResponse<String>> cancelMyPayment(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @PathVariable String paymentId
    ) {
        log.info("Cancel payment with id: {}", paymentId);
        
        paymentService.cancelMyPayment(
            userId,
            paymentId
        );

        return ResponseEntity.ok(ApiResponse.success("Cancel payment successfully", null));
    }
}