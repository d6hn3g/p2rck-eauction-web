package com.github.dghng36.eauction.modules.finance.wallet.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class WalletResponse {
    String id;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    BigDecimal availableBalance;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    BigDecimal holdBalance;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    BigDecimal totalBalance;

    Instant createdAt;
    Instant updatedAt;
}
