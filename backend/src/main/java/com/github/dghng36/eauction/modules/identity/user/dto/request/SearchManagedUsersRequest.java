package com.github.dghng36.eauction.modules.identity.user.dto.request;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.AssertTrue;
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
public class SearchManagedUsersRequest extends SearchPublicUsersRequest{

    List<String> roles;
    List<String> statuses;

    Instant createdBefore;
    Instant createdAfter;

    @AssertTrue(message = "createdAfter must be before createdBefore")
    public boolean isCreatedAtRangedValid() {
        if (createdAfter == null || createdBefore == null) {
            return true;
        }
        return createdAfter.isBefore(createdBefore);
    }

}
