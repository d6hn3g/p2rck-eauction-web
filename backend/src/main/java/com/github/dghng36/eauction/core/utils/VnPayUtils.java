package com.github.dghng36.eauction.core.utils;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class VnPayUtils {
    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();
    private static final String HMAC_SHA512 = "HmacSHA512";

    public static String hmacSHA512(final String key, final String data) {
        if (key == null || data == null) {
            throw new IllegalArgumentException("Key and data must not be null");
        }
        
        try {
            Mac hmac512 = Mac.getInstance(HMAC_SHA512);
            SecretKeySpec secretKey = new SecretKeySpec(
                key.getBytes(StandardCharsets.UTF_8), 
                HMAC_SHA512
            );
            hmac512.init(secretKey);
            
            byte[] result = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            
            char[] hexChars = new char[result.length * 2];
            for (int j = 0; j < result.length; j++) {
                int v = result[j] & 0xFF;
                hexChars[j * 2] = HEX_ARRAY[v >>> 4];
                hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
            }
            
            return new String(hexChars);
            
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to calculate HMAC-SHA512 signature", e);
        }
    }

    public static boolean validateSignature(String hashSecret, Map<String, String> params, String vnpSecureHash) {
        String calculatedHash = hmacSHA512(hashSecret, buildHashData(params));
        return calculatedHash.equalsIgnoreCase(vnpSecureHash);
    }

    private static String buildHashData(Map<String, String> params) {
        StringBuilder hashData = new StringBuilder();
        params.forEach((k, v) -> {
            if (!k.equals("vnp_SecureHash") && v != null && !v.isEmpty()) {
                hashData.append(k).append('=').append(v).append('&');
            }
        });
        if (hashData.length() > 0) {
            hashData.setLength(hashData.length() - 1);
        }
        return hashData.toString();
    }
}
