package com.github.dghng36.eauction.modules.finance.payment.controller.v1;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.dghng36.eauction.modules.finance.payment.dto.request.SePayWebhookRequest;
import com.github.dghng36.eauction.modules.finance.payment.service.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api/v1/payments/callback")
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class PaymentCallbackController {
    PaymentService paymentService;

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @PostMapping("/sepay")
    ResponseEntity<Map<String, Object>> handleSePayWebhook(
        @RequestBody SePayWebhookRequest sePayWebhookRequest
    ) {
        paymentService.handleSePayWebhook(sePayWebhookRequest);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "SePay webhook handled successfully");

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @RequestMapping(
        value = "/vnpay", 
        method = {RequestMethod.GET, RequestMethod.POST}
    )
    public ResponseEntity<Map<String, String>> handleVNPayCallback(
        @RequestParam Map<String, String> params
    ) {
        Map<String, String> response = paymentService.handleVNPayCallback(params);
        
        return ResponseEntity.ok(response); 
    }
}
