package com.flamingo.qa.pojo.booking;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;

@Builder(toBuilder = true)
@JsonNaming(PropertyNamingStrategies.LowerCaseStrategy.class)
public record Booking(String firstname,
                      String lastname,
                      Integer totalPrice,
                      Boolean depositPaid,
                      BookingDates bookingDates,
                      String additionalNeeds) {
}
