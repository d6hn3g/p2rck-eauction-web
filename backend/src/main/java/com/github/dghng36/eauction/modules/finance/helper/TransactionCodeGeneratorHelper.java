package com.github.dghng36.eauction.modules.finance.helper;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class TransactionCodeGeneratorHelper {
    public static String generateTransactionCode() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmssSSS"));
    
        SecureRandom random = new SecureRandom();
        int randomNumber = 1000 + random.nextInt(9000); 
        
        return "TX" + timestamp + randomNumber;
    }
}
