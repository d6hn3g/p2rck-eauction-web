package com.github.dghng36.eauction.modules.finance.transactions.dto.request;

import java.util.Map;

import com.github.dghng36.eauction.core.utils.ConstantsUtils;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class UpdateTransactionRequest {
    String newStatus;

    @Size(
        max = ConstantsUtils.FinanceConstants.MAX_DESCRIPTION_LENGTH,
        message = "Description must not exceed " + ConstantsUtils.FinanceConstants.MAX_DESCRIPTION_LENGTH + " characters"
    )
    String description;

    @Size(
        max = ConstantsUtils.MetadataConstants.MAX_METADATA_SIZE,
        message = "Metadata must not contain more than " + ConstantsUtils.MetadataConstants.MAX_METADATA_SIZE + " entries"
    )
    Map<String, Object> metadata;
}
