package com.github.dghng36.eauction.modules.finance.bankAccount.controller.v1;

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
import com.github.dghng36.eauction.modules.finance.bankAccount.dto.request.SearchBankAccountsRequest;
import com.github.dghng36.eauction.modules.finance.bankAccount.dto.response.BankAccountResponse;
import com.github.dghng36.eauction.modules.finance.bankAccount.service.BankAccountService;

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
public class AdminBankAccountController {
    BankAccountService bankAccountService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/bank-accounts")
    @Validated
    ResponseEntity<ApiResponse<PageResponse<BankAccountResponse>>> getAllBankAccounts(
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
        PageResponse<BankAccountResponse> bankAccountResponses = bankAccountService.getAllBankAccounts(
            page, size, 
            sortBy, sortDirection
        );
        return ResponseEntity.ok(ApiResponse.success("Bank accounts retrieved successfully", bankAccountResponses));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/bank-accounts/search")
    @Validated
    ResponseEntity<ApiResponse<PageResponse<BankAccountResponse>>> searchAllBankAccounts(
        @RequestBody SearchBankAccountsRequest searchBankAccountsRequest,
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
        
        PageResponse<BankAccountResponse> bankAccountResponses = bankAccountService.searchAllBankAccounts(
            searchBankAccountsRequest, 
            page, size, 
            sortBy, sortDirection
        );
        
        return ResponseEntity.ok(ApiResponse.success("Bank accounts retrieved successfully", bankAccountResponses));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/{userId}/bank-accounts/{bankAccountId}")
    ResponseEntity<ApiResponse<BankAccountResponse>> getBankAccount(
        @PathVariable String userId,
        @PathVariable String bankAccountId
    ) {
        BankAccountResponse bankAccountResponse = bankAccountService.getUserBankAccount(userId, bankAccountId);

        return ResponseEntity.ok(ApiResponse.success("Bank account retrieved successfully", bankAccountResponse));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/bank-accounts/{bankAccountId}/verify")
    ResponseEntity<ApiResponse<Void>> verifyBankAccount(
        @PathVariable String bankAccountId
    ) {
        log.info("Verifying bank account with ID: [{}]", bankAccountId);
        
        bankAccountService.verifyBankAccount(bankAccountId);

        return ResponseEntity.ok(ApiResponse.success("Bank account verified successfully"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/bank-accounts/{bankAccountId}/reject")
    ResponseEntity<ApiResponse<Void>> rejectBankAccount(
        @PathVariable String bankAccountId
    ) {
        log.info("Rejecting bank account with ID: [{}]", bankAccountId);

        bankAccountService.rejectBankAccount(bankAccountId);

        return ResponseEntity.ok(ApiResponse.success("Bank account rejected successfully"));
    }
    
    
}
