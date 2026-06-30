package com.github.dghng36.eauction.modules.auction.auctionRoom.dto.request;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class SearchAuctionRoomsRequest {
    String searchQuery;

    List<String> statuses;

    Instant startTimeFrom;
    Instant startTimeTo;

    Instant createdAtFrom;
    Instant createdAtTo;

    Boolean hasSlotted;

    Integer minTotalParticipants;
    Integer maxTotalParticipants;

    @Positive(message = "minCurrentParticipants must be a positive integer")
    @Min(
        value = 0, 
        message = "minCurrentParticipants must be greater than or equal to 0"
    )
    Integer minCurrentParticipants;

    @Positive(message = "maxCurrentParticipants must be a positive integer")
    @Min(
        value = 0, 
        message = "maxCurrentParticipants must be greater than or equal to 0"
    )
    Integer maxCurrentParticipants; // Add Max later

    @AssertTrue(message = "startTimeFrom must be before startTimeTo")
    public boolean isStartTimeRangeValid() {
        if (startTimeFrom == null || startTimeTo == null) {
            return true;
        }
        return startTimeFrom.isBefore(startTimeTo);
    }

    @AssertTrue(message = "createdAtFrom must be before createdAtTo")
    public boolean isCreatedAtRangeValid() {
        if (createdAtFrom == null || createdAtTo == null) {
            return true;
        }
        return createdAtFrom.isBefore(createdAtTo);
    }
}
