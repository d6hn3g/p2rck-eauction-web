package com.github.dghng36.eauction.modules.finance.bankAccount.listener;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.github.dghng36.eauction.modules.finance.bankAccount.event.RejectedBankAccountEvent;
import com.github.dghng36.eauction.modules.finance.bankAccount.event.VerifiedBankAccountEvent;
import com.github.dghng36.eauction.modules.notification.enums.NotificationType;
import com.github.dghng36.eauction.modules.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class BankAccountNotificationListener {
    NotificationService notificationService;

    @Async
    @EventListener
    public void handleVerifiedBankAccountEven(
        VerifiedBankAccountEvent event
    ) {
        try {
            notificationService.createNotification(
                event.getUserId(), 
                NotificationType.VERIFIED_BANK_ACCOUNT, 
                "Bank Account Verified", 
                "Your bank account has verified by system", 
                event.getBankAccountId(), 
                null
            );
        } catch(Exception ex) {
            log.error("Error during create new notification VERIFY_BANK_ACCOUNT, error: [{}]: ", ex.getMessage(), ex);
        }
    }

    @Async
    @EventListener
    public void handleRejectedBankAccountEven(
        RejectedBankAccountEvent event
    ) {
        try {
            notificationService.createNotification(
                event.getUserId(), 
                NotificationType.VERIFIED_BANK_ACCOUNT, 
                "Bank Account Rejected", 
                "Your bank account has rejected by system because some reason", 
                event.getBankAccountId(), 
                null
            );
        } catch(Exception ex) {
            log.error("Error during create new notification REJECTED_BANK_ACCOUNT, error: [{}]: ", ex.getMessage(), ex);
        }
    }
}
