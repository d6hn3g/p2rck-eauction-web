package com.github.dghng36.eauction.modules.finance.helper;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class BankAccountMaskedHelper {
    public static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return accountNumber; // Return as is if null or too short to mask
        }

        int lengthToMask = accountNumber.length() - 4;
        
        String maskedPart = "*".repeat(lengthToMask);
        
        String visiblePart = accountNumber.substring(lengthToMask);
        
        return maskedPart + visiblePart;
    }
}
