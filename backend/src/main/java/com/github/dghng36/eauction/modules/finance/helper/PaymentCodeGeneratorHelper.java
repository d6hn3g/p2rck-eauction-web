package com.github.dghng36.eauction.modules.finance.helper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class PaymentCodeGeneratorHelper {
    public static String generatePaymentCode(String prefix) {
        String cleanPrefix = (prefix != null && !prefix.trim().isEmpty()) ? prefix.trim().toUpperCase() + "_" : "";
    
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
    
        String randomSuffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    
        return cleanPrefix + timestamp + "_" + randomSuffix;
    }
}
