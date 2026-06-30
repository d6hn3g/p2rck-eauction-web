package com.github.dghng36.eauction.modules.finance.wallet.controller.v1;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.dghng36.eauction.core.base.ApiResponse;
import com.github.dghng36.eauction.modules.finance.wallet.dto.request.CreditUserWalletRequest;
import com.github.dghng36.eauction.modules.finance.wallet.dto.request.DebitUserWalletRequest;
import com.github.dghng36.eauction.modules.finance.wallet.service.WalletService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;


@RestController
@RequestMapping("/api/v1/admin/management")
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AdminWalletController {
    WalletService walletService;

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/users/{userId}/wallets/credit")
    ResponseEntity<ApiResponse<Void>> creditUserWallet(
        @PathVariable String userId,
        @Valid @RequestBody CreditUserWalletRequest creditUserWalletRequest
    ) {
        log.info("Admin is crediting user wallet with user: [{}]", userId);
        
        walletService.creditUserWallet(userId, creditUserWalletRequest);
        
        return ResponseEntity.ok(ApiResponse.success("User wallet credited successfully"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/users/{userId}/wallets/debit")
    ResponseEntity<ApiResponse<Void>> debitUserWallet(
        @PathVariable String userId,
        @Valid @RequestBody DebitUserWalletRequest debitUserWalletRequest
    ) {
        log.info("Admin is debiting user wallet with user: [{}]", userId);

        walletService.debitUserWallet(userId, debitUserWalletRequest);
        
        return ResponseEntity.ok(ApiResponse.success("User wallet debited successfully"));
    }
    
}
