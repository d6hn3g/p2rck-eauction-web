package com.github.dghng36.eauction.modules.finance.bankAccount.controller.v1;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import com.github.dghng36.eauction.modules.finance.bankAccount.dto.request.CreateBankAccountRequest;
import com.github.dghng36.eauction.modules.finance.bankAccount.dto.request.SearchBankAccountsRequest;
import com.github.dghng36.eauction.modules.finance.bankAccount.dto.request.UpdateBankAccountRequest;
import com.github.dghng36.eauction.modules.finance.bankAccount.dto.response.BankAccountResponse;
import com.github.dghng36.eauction.modules.finance.bankAccount.service.BankAccountService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/users/me/bank-accounts")
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class MyBankAccountController {
    BankAccountService bankAccountService;


    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @PostMapping
    ResponseEntity<ApiResponse<BankAccountResponse>> createMyBankAccount(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @Valid @RequestBody CreateBankAccountRequest createBankAccountRequest
    ) {
        log.info("Creating bank account for user: [{}]", userId);
        
        BankAccountResponse bankAccountResponse = bankAccountService.createMyBankAccount(
            userId, createBankAccountRequest
        );

        return ResponseEntity.ok(ApiResponse.success("Bank account created successfully", bankAccountResponse));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @GetMapping
    @Validated
    ResponseEntity<ApiResponse<PageResponse<BankAccountResponse>>> getMyBankAccounts(
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
        PageResponse<BankAccountResponse> bankAccountResponses = bankAccountService.getMyBankAccounts(
            userId,
            page, size, 
            sortBy, sortDirection
        );
        return ResponseEntity.ok(ApiResponse.success("Bank accounts retrieved successfully", bankAccountResponses));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @PostMapping("/search")
    @Validated
    ResponseEntity<ApiResponse<PageResponse<BankAccountResponse>>> searchMyBankAccounts(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @RequestBody SearchBankAccountsRequest searchBankAccountRequest,
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
        PageResponse<BankAccountResponse> bankAccountResponses = bankAccountService.searchMyBankAccounts(
            userId, searchBankAccountRequest,
            page, size, 
            sortBy, sortDirection
        );

        return ResponseEntity.ok(ApiResponse.success("Bank accounts retrieved successfully", bankAccountResponses));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @GetMapping("/{bankAccountId}")
    ResponseEntity<ApiResponse<BankAccountResponse>> getMyBankAccount(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @PathVariable String bankAccountId
    ) {
        BankAccountResponse bankAccountResponse = bankAccountService.getUserBankAccount(
            userId, bankAccountId
        );
        
        return ResponseEntity.ok(ApiResponse.success("Bank account retrieved successfully", bankAccountResponse));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @PostMapping("/{bankAccountId}")
    ResponseEntity<ApiResponse<BankAccountResponse>> updateMyBankAccount(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @PathVariable String bankAccountId,
        @Valid @RequestBody UpdateBankAccountRequest updateBankAccountRequest
    ) {
        log.info("Updating bank account with id: [{}] for user: [{}]", bankAccountId, userId);

        BankAccountResponse bankAccountResponse = bankAccountService.updateMyBankAccount(
            userId, 
            bankAccountId, 
            updateBankAccountRequest
        );

        return ResponseEntity.ok(ApiResponse.success("Bank account updated successfully", bankAccountResponse));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @DeleteMapping("/{bankAccountId}")
    ResponseEntity<ApiResponse<Void>> deleteMyBankAccount(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @PathVariable String bankAccountId
    ) {
        log.info("Deleting bank account with id: [{}] for user: [{}]", bankAccountId, userId);
        
        bankAccountService.deleteMyBankAccount(userId, bankAccountId);

        return ResponseEntity.ok(ApiResponse.success("Bank account deleted successfully"));
    }
}
