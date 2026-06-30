package com.github.dghng36.eauction.modules.identity.helper;

import java.util.Base64;

import org.springframework.stereotype.Component;

@Component
public class PIICryptoHelper {
    public String encodeNationalId(String nationalId) {
        Base64.Encoder encoder = Base64.getEncoder();
        return encoder.encodeToString(nationalId.getBytes());
    }

    public String decodeNationalId(String encodedNationalId) {
        Base64.Decoder decoder = Base64.getDecoder();
        return new String(decoder.decode(encodedNationalId));
    }
}
