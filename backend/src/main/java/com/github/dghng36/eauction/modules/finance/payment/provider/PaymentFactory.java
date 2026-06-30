package com.github.dghng36.eauction.modules.finance.payment.provider;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.modules.finance.enums.PaymentProvider;

import lombok.experimental.FieldDefaults;

@Component
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class PaymentFactory {
    Map<PaymentProvider, IPaymentProvider> paymentProviders;

    public PaymentFactory(
        List<IPaymentProvider> paymentProviderList
    ) {
        this.paymentProviders = paymentProviderList.stream()
            .collect(
                Collectors.toMap(
                    paymentProvider -> paymentProvider.getProvider(), 
                    Function.identity())
            );
    }

    public IPaymentProvider getPaymentProvider(
        PaymentProvider provider
    ) {
        if (provider == null || !paymentProviders.containsKey(provider)) {
            throw new AppException("Payment provider not supported: " + provider, HttpStatus.BAD_REQUEST);
        }
        return paymentProviders.get(provider);
    }
}
