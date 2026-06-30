package com.github.dghng36.eauction.modules.identity.reputation.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class WelcomeAwardedBonusEvent {
    String userId;

    Double awardedAmount;
}
