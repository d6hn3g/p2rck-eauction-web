package com.github.dghng36.eauction.modules.finance.wallet.mapper;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.github.dghng36.eauction.modules.finance.wallet.dto.response.WalletResponse;
import com.github.dghng36.eauction.modules.finance.wallet.model.Wallet;

@Component
public class WalletMapper {
    public Wallet toWalletEntity(String userId) {
        return Wallet.builder()
            .userId(userId)
            .availableBalance(BigDecimal.ZERO)
            .holdBalance(BigDecimal.ZERO)
            .isDeleted(false)
            .deletedAt(null)
            .build();
    }

    public WalletResponse toWalletResponse(Wallet wallet) {
        if (wallet == null) {
            return null;
        }

        return WalletResponse.builder()
            .id(wallet.getId())
            .availableBalance(wallet.getAvailableBalance())
            .holdBalance(wallet.getHoldBalance())
            .totalBalance(wallet.getAvailableBalance().add(wallet.getHoldBalance()))
            .createdAt(wallet.getCreatedAt())
            .updatedAt(wallet.getUpdatedAt())
            .build();
    }
}
