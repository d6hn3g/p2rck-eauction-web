package com.github.dghng36.eauction.modules.identity.user.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
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
public class SearchPublicUsersRequest {
    String searchQuery; // General search query that can match against multiple fields: username, email, fullName, phoneNumber, address

    String nationality;

    Integer yearOfBirth;

    @Min(value = 0, message = "minReputation must be greater than or equal to 0")
    Double minReputation;
    
    @Min(value = 0, message = "maxReputation must be greater than or equal to 0")
    Double maxReputation; // Add max later

    @AssertTrue(message = "maxReputation must be greater than or equal to minReputation")
    public boolean isReputationRangeValid() {
        if (minReputation != null && maxReputation != null) {
            return maxReputation >= minReputation;
        }
        return true;
    }
}
