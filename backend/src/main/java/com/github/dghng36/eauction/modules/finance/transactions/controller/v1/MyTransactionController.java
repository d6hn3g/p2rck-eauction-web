package com.github.dghng36.eauction.modules.finance.transactions.controller.v1;

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
import com.github.dghng36.eauction.modules.finance.transactions.dto.request.SearchTransactionsRequest;
import com.github.dghng36.eauction.modules.finance.transactions.dto.response.TransactionResponse;
import com.github.dghng36.eauction.modules.finance.transactions.service.TransactionService;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;


@RestController
@RequestMapping("/api/v1/users/me/transactions")
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class MyTransactionController {
    TransactionService transactionService;

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @GetMapping
    @Validated
    ResponseEntity<ApiResponse<PageResponse<TransactionResponse>>> getMyTransactions(
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
        PageResponse<TransactionResponse> transactionResponses = transactionService.getMyTransactions(
            userId,
            page, size,
            sortBy, sortDirection
        );

        return ResponseEntity.ok(ApiResponse.success("Retrieved user transactions successfully", transactionResponses));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @PostMapping
    @Validated
    ResponseEntity<ApiResponse<PageResponse<TransactionResponse>>> searchMyTransactions(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @RequestBody SearchTransactionsRequest searchTransactionsRequest,
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
        PageResponse<TransactionResponse> transactionResponses = transactionService.searchMyTransactions(
            userId,
            searchTransactionsRequest,
            page, size,
            sortBy, sortDirection
        );

        return ResponseEntity.ok(ApiResponse.success("Searched user transactions successfully", transactionResponses));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @GetMapping("/{transactionId}")
    ResponseEntity<ApiResponse<TransactionResponse>> getMyTransaction(
        @PathVariable String transactionId
    ) {
        TransactionResponse transactionResponse = transactionService.getMyTransaction(transactionId);
        return ResponseEntity.ok(ApiResponse.success("Retrieved user transaction successfully", transactionResponse));
    }
    
}
