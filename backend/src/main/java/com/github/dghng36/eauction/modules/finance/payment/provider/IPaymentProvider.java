package com.github.dghng36.eauction.modules.finance.payment.provider;

import java.util.Map;

import com.github.dghng36.eauction.modules.finance.enums.PaymentProvider;
import com.github.dghng36.eauction.modules.finance.payment.dto.response.CreateDepositResponse;
import com.github.dghng36.eauction.modules.finance.payment.model.Payment;

public interface IPaymentProvider {
    PaymentProvider getProvider();

    CreateDepositResponse createDepositPayment(
        String userId,
        Payment payment
    );

    void executePayout(Payment payment);

    boolean validateCallback(Map<String, String> params);
}
