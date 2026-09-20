package com.flamingo.qa.api.model.booking;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/** {@code POST /booking} is the only endpoint that wraps the booking; GET and PUT return it bare. */
@Value
@Builder
@Jacksonized
@JsonNaming(PropertyNamingStrategies.LowerCaseStrategy.class)
public class BookingResponse {

    Integer bookingId;
    Booking booking;
}
