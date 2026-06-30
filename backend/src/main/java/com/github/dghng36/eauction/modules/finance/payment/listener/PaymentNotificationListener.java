package com.github.dghng36.eauction.modules.finance.payment.listener;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.github.dghng36.eauction.modules.finance.payment.event.DepositEvent;
import com.github.dghng36.eauction.modules.finance.payment.event.WithdrawApprovedEvent;
import com.github.dghng36.eauction.modules.finance.payment.event.WithdrawCreatedEvent;
import com.github.dghng36.eauction.modules.finance.payment.event.WithdrawRejectedEvent;
import com.github.dghng36.eauction.modules.notification.enums.NotificationType;
import com.github.dghng36.eauction.modules.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class PaymentNotificationListener {
    NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDepositSuccessEvent(
        DepositEvent event
    ) {
        notificationService.createNotification(
            event.getUserId(),
            NotificationType.SYSTEM,
            "Deposit successful",
            event.getDescription(),
            event.getPaymentId(),
            Map.of(
                "paymentCode", event.getPaymentCode(),
                "amount", event.getAmount()
            )
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleWithdrawCreatedEvent(
        WithdrawCreatedEvent event
    ) {
        notificationService.createNotification(
            event.getUserId(),
            NotificationType.SYSTEM,
            "Withdraw Request Created",
            event.getDescription(),
            event.getPaymentId(),
            Map.of(
                "bankAccountId", event.getBankAccountId(),
                "bankName", event.getBankName(),
                "accountNumber", event.getAccountNumber(),
                "accountHolderName", event.getAccountHolderName(),
                "paymentCode", event.getPaymentCode(),
                "amount", event.getAmount()
            )
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleWithdrawApprovedEvent(
        WithdrawApprovedEvent event
    ) {
        notificationService.createNotification(
            event.getUserId(),
            NotificationType.SYSTEM,
            "Withdraw Request Approved",
            event.getDescription(),
            event.getPaymentId(),
            Map.of(
                "bankAccountId", event.getBankAccountId(),
                "bankName", event.getBankName(),
                "accountNumber", event.getAccountNumber(),
                "accountHolderName", event.getAccountHolderName(),
                "paymentCode", event.getPaymentCode(),
                "amount", event.getAmount()
            )
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleWithdrawRejectedEvent(
        WithdrawRejectedEvent event
    ) {
        notificationService.createNotification(
            event.getUserId(),
            NotificationType.SYSTEM,
            "Withdraw Request Rejected",
            event.getDescription(),
            event.getPaymentId(),
            Map.of(
                "bankAccountId", event.getBankAccountId(),
                "bankName", event.getBankName(),
                "accountNumber", event.getAccountNumber(),
                "accountHolderName", event.getAccountHolderName(),
                "paymentCode", event.getPaymentCode(),
                "amount", event.getAmount()
            )
        );
    }
}
