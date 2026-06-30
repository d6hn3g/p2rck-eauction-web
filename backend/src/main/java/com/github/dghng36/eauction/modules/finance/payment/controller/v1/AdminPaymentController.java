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
import com.github.dghng36.eauction.modules.finance.payment.dto.request.ApproveWithdrawRequest;
import com.github.dghng36.eauction.modules.finance.payment.dto.request.RejectWithdrawRequest;
import com.github.dghng36.eauction.modules.finance.payment.dto.request.SearchPaymentRequest;
import com.github.dghng36.eauction.modules.finance.payment.dto.response.PaymentAdminResponse;
import com.github.dghng36.eauction.modules.finance.payment.dto.response.PaymentStatusResponse;
import com.github.dghng36.eauction.modules.finance.payment.service.PaymentService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/admin/management")
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AdminPaymentController {
    PaymentService paymentService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/payments")
    @Validated
    ResponseEntity<ApiResponse<PageResponse<PaymentAdminResponse>>> getAllPayments(
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
        PageResponse<PaymentAdminResponse> paymentResponses = paymentService.getAllPayments(
            page, size,
            sortBy, sortDirection
        );

        return ResponseEntity.ok(ApiResponse.success("Get all payments successfully", paymentResponses));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/payments/search")
    @Validated
    ResponseEntity<ApiResponse<PageResponse<PaymentAdminResponse>>> searchAllPayments(
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
        PageResponse<PaymentAdminResponse> paymentResponses = paymentService.searchAllPayments(
            searchPaymentRequest,
            page, size,
            sortBy, sortDirection
        );

        return ResponseEntity.ok(ApiResponse.success("Search payments successfully", paymentResponses));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/userId/{userId}/payments/{paymentId}")
    ResponseEntity<ApiResponse<PaymentAdminResponse>> getUserPayment(
        @PathVariable String userId,
        @PathVariable String paymentId
    ) {
        PaymentAdminResponse paymentResponse = paymentService.getUserPayment(userId, paymentId);

        return ResponseEntity.ok(ApiResponse.success("Get payment successfully", paymentResponse));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/{userId}/payments/{paymentId}/status")
    ResponseEntity<ApiResponse<PaymentStatusResponse>> getPaymentStatus(
        @PathVariable String userId,
        @PathVariable String paymentId
    ) {
        PaymentStatusResponse paymentStatusResponse = paymentService.getUserPaymentStatus(userId, paymentId);

        return ResponseEntity.ok(ApiResponse.success("Get payment status successfully", paymentStatusResponse));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{paymentId}/approve")
    ResponseEntity<ApiResponse<Void>> approveWithdrawPayment(
        @PathVariable String paymentId,
        @RequestBody ApproveWithdrawRequest approveWithdrawRequest
    ) {
        log.info("Approve withdraw payment with id: {}", paymentId);
        
        paymentService.approveWithdrawPayment(paymentId, approveWithdrawRequest);

        return ResponseEntity.ok(ApiResponse.success("Approve withdraw payment successfully", null));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{paymentId}/reject")
    ResponseEntity<ApiResponse<Void>> rejectWithdrawPayment(
        @PathVariable String paymentId,
        @Valid @RequestBody RejectWithdrawRequest rejectWithdrawRequest
    ) {
        log.info("Reject withdraw payment with id: {}", paymentId);
        
        paymentService.rejectWithdrawPayment(paymentId, rejectWithdrawRequest);

        return ResponseEntity.ok(ApiResponse.success("Reject withdraw payment successfully", null));
    }
}
