package com.github.dghng36.eauction.modules.finance.payment.provider.sepay;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.github.dghng36.eauction.modules.finance.enums.PaymentProvider;
import com.github.dghng36.eauction.modules.finance.payment.dto.response.CreateDepositResponse;
import com.github.dghng36.eauction.modules.finance.payment.model.Payment;
import com.github.dghng36.eauction.modules.finance.payment.provider.IPaymentProvider;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class SePayProvider implements IPaymentProvider {

    RestTemplate restTemplate;
    
    @NonFinal
    @Value("${app.payment.sepay.api-url:https://api.sepay.vn}") 
    String apiUrl;

    @NonFinal
    @Value("${app.payment.sepay.api-key}") 
    String apiKey;
    
    // My bank account info for receiving payments
    @NonFinal
    @Value("${app.payment.sepay.company-bank-account}") 
    String companyAccount;

    @NonFinal
    @Value("${app.payment.sepay.company-bank-name}") 
    String companyBank;

    @Override
    public PaymentProvider getProvider() {
        return PaymentProvider.SE_PAY;
    }

    @Override
    public CreateDepositResponse createDepositPayment(
        String userId,
        Payment payment
    ) {
        String qrContent = String.format("https://img.vietqr.io/image/%s-%s-compact.png?amount=%s&addIn=%s", 
            companyBank, companyAccount, payment.getAmount().toBigInteger(), payment.getPaymentCode()
        );

        return CreateDepositResponse.builder()
            .paymentId(payment.getId())
            .paymentCode(payment.getPaymentCode())
            .paymentUrl(qrContent)
            .paymentStatus(payment.getStatus().name())
            .createdAt(payment.getCreatedAt())
            .build();
    }

    @Override
    public void executePayout(Payment payment) {}

    @Override
    public boolean validateCallback(Map<String, String> params) { return false; }
}
