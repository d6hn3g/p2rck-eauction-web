package com.github.dghng36.eauction.modules.finance.wallet.controller.v1;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.dghng36.eauction.core.base.ApiResponse;
import com.github.dghng36.eauction.infra.config.security.annotation.AuthInfo;
import com.github.dghng36.eauction.infra.config.security.annotation.AuthInfoType;
import com.github.dghng36.eauction.modules.finance.wallet.dto.response.WalletResponse;
import com.github.dghng36.eauction.modules.finance.wallet.service.WalletService;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;


@RestController
@RequestMapping("/api/v1/users/me/wallet")
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class MyWalletController {

    WalletService walletService;

    @GetMapping
    ResponseEntity<ApiResponse<WalletResponse>> getMyWallet(
        @AuthInfo(info = AuthInfoType.ID) String userId
    ) {
        WalletResponse walletResp = walletService.getMyWallet(userId);

        return ResponseEntity.ok(ApiResponse.success("Get my wallet successfully", walletResp));
    }
}
