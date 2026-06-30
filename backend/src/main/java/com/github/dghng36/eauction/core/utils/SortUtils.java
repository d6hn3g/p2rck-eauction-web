package com.github.dghng36.eauction.core.utils;

import java.util.Set;

import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class SortUtils {
    private static String resolveField(String sortBy, Set<String> allowedFields) {
        return resolveField(sortBy, allowedFields, "createdAt");
    }

    private static String resolveField(String sortBy, Set<String> allowedFields, String defaultField) {
        if (!StringUtils.hasText(sortBy) || !allowedFields.contains(sortBy)) {
            return defaultField;
        }
        return sortBy;
    }

    private static Sort.Direction resolveDirection(String sortDirection) {
        if (sortDirection == null) {
            return Sort.Direction.DESC;
        }
        
        return switch(sortDirection.trim().toLowerCase()) {
            case "asc" -> Sort.Direction.ASC;
            default -> Sort.Direction.DESC;
        };
    }

    public static Sort buildSort(String sortBy, String sortDirection, Set<String> allowedFields) {
        String validatedSortBy = resolveField(sortBy, allowedFields);
        Sort.Direction validatedSortDirection = resolveDirection(sortDirection);
        return Sort.by(validatedSortDirection, validatedSortBy);
    }

    public static Sort buildSort(String sortBy, String sortDirection, Set<String> allowedFields, String defaultSortBy) {
        String validatedSortBy = resolveField(sortBy, allowedFields, defaultSortBy);
        Sort.Direction validatedSortDirection = resolveDirection(sortDirection);
        return Sort.by(validatedSortDirection, validatedSortBy);
    }
}
