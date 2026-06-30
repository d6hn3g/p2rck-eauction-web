package com.github.dghng36.eauction.core.utils;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;

import com.github.dghng36.eauction.core.exception.AppException;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class MetadataUtils {
    public static Map<String, Object> sanitizeDynamicMetadata(Map<String, Object> metadata) {
        // If metadata is null, return an empty map
        if (metadata == null) {
            return new LinkedHashMap<>();
        }

        // Check if the metadata size exceeds the maximum allowed size
        if (metadata.size() > ConstantsUtils.MetadataConstants.MAX_METADATA_SIZE) {
            throw new AppException("Metadata size exceeds the maximum allowed limit of " + ConstantsUtils.MetadataConstants.MAX_METADATA_SIZE, HttpStatus.BAD_REQUEST);
        }

        // Sanitize the metadata by removing any entries with null values or invalid keys input
        Map<String, Object> sanitizeMetadata = new LinkedHashMap<>();
        metadata.forEach((key, value) -> {
            String sanitizeKey = key.replaceAll("[.$]", "_").trim();
            
            if (!sanitizeKey.isEmpty() && value != null) {
                if (value instanceof String s) {
                    if (s.length() > ConstantsUtils.MetadataConstants.MAX_VALUE_LENGTH) {
                        throw new AppException("Metadata value for key '" + key + "' exceeds the maximum allowed length of " + ConstantsUtils.MetadataConstants.MAX_VALUE_LENGTH, HttpStatus.BAD_REQUEST);
                    }
                }
                sanitizeMetadata.put(sanitizeKey, value instanceof String s ? s.trim() : value);
            }
        });
        return sanitizeMetadata;
    }
}
