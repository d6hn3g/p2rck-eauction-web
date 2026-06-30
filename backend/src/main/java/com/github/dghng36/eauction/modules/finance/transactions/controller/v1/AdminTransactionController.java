package com.github.dghng36.eauction.modules.finance.transactions.controller.v1;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.dghng36.eauction.core.base.ApiResponse;
import com.github.dghng36.eauction.core.base.PageResponse;
import com.github.dghng36.eauction.modules.finance.transactions.dto.request.SearchTransactionsRequest;
import com.github.dghng36.eauction.modules.finance.transactions.dto.request.UpdateTransactionRequest;
import com.github.dghng36.eauction.modules.finance.transactions.dto.response.TransactionAdminResponse;
import com.github.dghng36.eauction.modules.finance.transactions.service.TransactionService;

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
public class AdminTransactionController {
    TransactionService transactionService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/transactions")
    @Validated
    ResponseEntity<ApiResponse<PageResponse<TransactionAdminResponse>>> getAllTransactions(
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
        PageResponse<TransactionAdminResponse> transactionResponses = transactionService.getAllTransactions(
            page, size,
            sortBy, sortDirection
        );

        return ResponseEntity.ok(ApiResponse.success("Retrieved transactions successfully", transactionResponses));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/transactions/search")
    @Validated
    ResponseEntity<ApiResponse<PageResponse<TransactionAdminResponse>>> searchAllTransactions(
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
        PageResponse<TransactionAdminResponse> transactionResponses = transactionService.searchAllTransactions(
            searchTransactionsRequest,
            page, size,
            sortBy, sortDirection
        );

        return ResponseEntity.ok(ApiResponse.success("Searched transactions successfully", transactionResponses));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/{userId}/transactions/{transactionId}")
    ResponseEntity<ApiResponse<TransactionAdminResponse>> getUserTransaction(
        @PathVariable String userId,
        @PathVariable String transactionId
    ) {
        TransactionAdminResponse transactionResponse = transactionService.getUserTransaction(userId, transactionId);
        return ResponseEntity.ok(ApiResponse.success("Retrieved user transaction successfully", transactionResponse));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/transactions/{transactionId}")
    ResponseEntity<ApiResponse<Void>> updateTransaction(
        @PathVariable String transactionId,
        @RequestBody UpdateTransactionRequest updateTransactionRequest
    ) {
        log.info("Admin updating transaction with id: [{}]", transactionId);

        transactionService.updateTransaction(transactionId, updateTransactionRequest);
        return ResponseEntity.ok(ApiResponse.success("Updated transaction successfully", null));
    }
}
