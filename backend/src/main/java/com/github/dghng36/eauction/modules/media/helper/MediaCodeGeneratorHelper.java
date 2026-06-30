package com.github.dghng36.eauction.modules.media.helper;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

@Component
public class MediaCodeGeneratorHelper {
    private static final String BASE_62 = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private static final int HASH_LENGTH = 8;

    private static final int RANDOM_SUFFIX_LENGTH = 3;

    private static final SecureRandom RANDOM = new SecureRandom();
    
    private static final Base62Helper base62Helper = new Base62Helper();

    public String generate(String source) {
        // Generate hash from source
        String hashed = DigestUtils.md5DigestAsHex(source.getBytes());

        // Take first 8 characters of the hash
        String hashPart = hashed.substring(0, HASH_LENGTH);

        // Convert hash part to a number
        long hashNum = Long.parseUnsignedLong(hashPart, 16);

        // Encode the number to base62
        String base62Encoded = base62Helper.encode(hashNum);

        // Return the base62 encoded string with a random suffix to ensure uniqueness
        return base62Encoded + generateRandomSuffix();
    }

    private String generateRandomSuffix() {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < RANDOM_SUFFIX_LENGTH; i++) {
            int index = RANDOM.nextInt(BASE_62.length());
            sb.append(BASE_62.charAt(index));
        }

        return sb.toString();
    }

    private static class Base62Helper {
        public String encode(long num) {
            StringBuilder sb = new StringBuilder();

            while (num > 0) {
                int remainder = (int)(num % 62);
                sb.append(BASE_62.charAt(remainder));
                num /= 62;
            }

            return sb.reverse().toString();
        }
    }
}
