package com.github.dghng36.eauction.modules.finance.payment.controller.v1;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.dghng36.eauction.core.base.ApiResponse;
import com.github.dghng36.eauction.infra.config.security.annotation.AuthInfo;
import com.github.dghng36.eauction.infra.config.security.annotation.AuthInfoType;
import com.github.dghng36.eauction.modules.finance.payment.dto.request.CreateDepositRequest;
import com.github.dghng36.eauction.modules.finance.payment.dto.request.CreateWithdrawRequest;
import com.github.dghng36.eauction.modules.finance.payment.dto.response.CreateDepositResponse;
import com.github.dghng36.eauction.modules.finance.payment.dto.response.CreateWithDrawResponse;
import com.github.dghng36.eauction.modules.finance.payment.service.PaymentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;


@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PaymentController {
    PaymentService paymentService;

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @PostMapping("/deposits")
    ResponseEntity<ApiResponse<CreateDepositResponse>> createDeposit(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @Valid @RequestBody CreateDepositRequest createDepositRequest
    ) {
        log.info("Create deposit with amount: {}", createDepositRequest.getAmount());
        
        CreateDepositResponse createDepositResp = paymentService.createDeposit(
            userId,
            createDepositRequest
        );
        
        return ResponseEntity.ok(ApiResponse.success("Create deposit successfully", createDepositResp));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @PostMapping("/withdrawals")
    ResponseEntity<ApiResponse<CreateWithDrawResponse>> createWithdraw(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @Valid @RequestBody CreateWithdrawRequest createWithdrawRequest
    ) {
        log.info("Create withdrawal with amount: {}", createWithdrawRequest.getAmount());
        
        CreateWithDrawResponse createWithdrawResp = paymentService.createWithdraw(
            userId,
            createWithdrawRequest
        );
        
        return ResponseEntity.ok(ApiResponse.success("Create withdrawal successfully", createWithdrawResp));
    }
}
