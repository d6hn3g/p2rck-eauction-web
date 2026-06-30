package com.github.dghng36.eauction.modules.finance.payment.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class WithdrawSuccessEvent extends PaymentEvent {
    String bankAccountId;
    String bankName;
    String accountNumber;
    String accountHolderName;
}
