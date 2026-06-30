package com.github.dghng36.eauction.modules.finance.transactions.dto.request;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class SearchTransactionsRequest {
    String searchQuery;

    List<String> type;
    List<String> status;

    Instant createdAtBefore;
    Instant createdAtAfter;

    Instant updatedAtBefore;
    Instant updatedAtAfter;
}
