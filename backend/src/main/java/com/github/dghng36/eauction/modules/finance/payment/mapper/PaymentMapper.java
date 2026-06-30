package com.github.dghng36.eauction.modules.finance.payment.mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.github.dghng36.eauction.core.utils.MetadataUtils;
import com.github.dghng36.eauction.modules.finance.enums.PaymentProvider;
import com.github.dghng36.eauction.modules.finance.enums.PaymentStatus;
import com.github.dghng36.eauction.modules.finance.enums.PaymentType;
import com.github.dghng36.eauction.modules.finance.helper.PaymentCodeGeneratorHelper;
import com.github.dghng36.eauction.modules.finance.payment.dto.response.PaymentAdminResponse;
import com.github.dghng36.eauction.modules.finance.payment.dto.response.PaymentResponse;
import com.github.dghng36.eauction.modules.finance.payment.dto.response.PaymentStatusResponse;
import com.github.dghng36.eauction.modules.finance.payment.model.Payment;

@Component
public class PaymentMapper {
    public Payment toDepositPaymentEntity(
        String userId, 
        PaymentProvider paymentProvider, 
        BigDecimal amount, String description, 
        Map<String, Object> metadata
    ) {
        if (paymentProvider == null || amount == null) {
            return null;
        }

        return Payment.builder()
            .paymentCode(PaymentCodeGeneratorHelper.generatePaymentCode(null))
            .userId(userId)
            .amount(amount)
            .provider(paymentProvider)
            .type(PaymentType.DEPOSIT)
            .status(PaymentStatus.PENDING)
            .bankAccountId(null)
            .failureReason(null)
            .description(description)
            .metadata(MetadataUtils.sanitizeDynamicMetadata(metadata))
            .callbackData(null)
            .isDeleted(false)
            .deletedAt(null)
            .build();
    }
    
    public Payment toWithdrawPaymentEntity(
        String userId, 
        PaymentProvider paymentProvider, 
        String bankAccountId,
        BigDecimal amount, String description, 
        Map<String, Object> metadata
    ) {
        if (paymentProvider == null || amount == null || bankAccountId == null) {
            return null;
        }

        return Payment.builder()
            .paymentCode(PaymentCodeGeneratorHelper.generatePaymentCode(null))
            .userId(userId)
            .amount(amount)
            .provider(paymentProvider)
            .type(PaymentType.WITHDRAW)
            .status(PaymentStatus.PENDING)
            .bankAccountId(bankAccountId)
            .failureReason(null)
            .description(description)
            .metadata(MetadataUtils.sanitizeDynamicMetadata(metadata))
            .callbackData(null)
            .isDeleted(false)
            .deletedAt(null)
            .build();
    }

    public PaymentResponse toPaymentResponse(Payment payment) {
        if (payment == null) {
            return null;
        }

        return PaymentResponse.builder()
            .userId(payment.getUserId())
            .paymentId(payment.getId())
            .paymentCode(payment.getPaymentCode())
            .paymentProvider(payment.getProvider().name())
            .paymentType(payment.getType().name())
            .paymentStatus(payment.getStatus().name())
            .amount(payment.getAmount())
            .providerTransactionId(payment.getProviderTransactionId())
            .failureReason(payment.getFailureReason())
            .metadata(payment.getMetadata())
            .description(payment.getDescription())
            .createdAt(payment.getCreatedAt())
            .updatedAt(payment.getUpdatedAt())
            .build();
    }

    public List<PaymentResponse> toPaymentResponseList(List<Payment> payments) {
        if (payments == null) {
            return List.of();
        }
        
        return payments.stream()
            .map(this::toPaymentResponse)
            .toList();
    }

    public PaymentStatusResponse toPaymentStatusResponse(Payment payment) {
        if (payment == null) {
            return null;
        }

        return PaymentStatusResponse.builder()
            .paymentId(payment.getId())
            .paymentCode(payment.getPaymentCode())
            .paymentStatus(payment.getStatus().name())
            .failureReason(payment.getFailureReason())
            .updatedAt(payment.getUpdatedAt())
            .build();
    }

    public PaymentAdminResponse toPaymentAdminResponse(Payment payment) {
        if (payment == null) {
            return null;
        }

        return PaymentAdminResponse.builder()
            .userId(payment.getUserId())
            .paymentId(payment.getId())
            .paymentCode(payment.getPaymentCode())
            .paymentProvider(payment.getProvider().name())
            .paymentType(payment.getType().name())
            .paymentStatus(payment.getStatus().name())
            .amount(payment.getAmount())
            .providerTransactionId(payment.getProviderTransactionId())
            .failureReason(payment.getFailureReason())
            .metadata(MetadataUtils.sanitizeDynamicMetadata(payment.getMetadata()))
            .callbackData(payment.getCallbackData())
            .createdAt(payment.getCreatedAt())
            .updatedAt(payment.getUpdatedAt())
            .build();
    }

    public List<PaymentAdminResponse> toPaymentAdminResponseList(List<Payment> payments) {
        if (payments == null) {
            return List.of();
        }
        return payments.stream()
            .map(this::toPaymentAdminResponse)
            .toList();
    }
}
