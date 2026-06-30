package com.github.dghng36.eauction.modules.finance.payment.event;

import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class RefundEvent extends PaymentEvent {}
