package com.flamingo.qa.api.model.booking;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * What {@code POST /booking} returns: the generated id alongside the stored booking.
 * Note that it is the only endpoint that wraps the booking - {@code GET} and {@code PUT}
 * return a bare {@link Booking}.
 */
@Value
@Builder
@Jacksonized
@JsonNaming(PropertyNamingStrategies.LowerCaseStrategy.class)
public class BookingResponse {

    Integer bookingId;
    Booking booking;
}
