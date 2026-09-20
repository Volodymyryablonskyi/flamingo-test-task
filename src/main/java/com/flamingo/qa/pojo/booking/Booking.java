package com.flamingo.qa.pojo.booking;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder(toBuilder = true)
@Jacksonized
@JsonNaming(PropertyNamingStrategies.LowerCaseStrategy.class)
public class Booking {

    String firstname;
    String lastname;
    Integer totalPrice;
    Boolean depositPaid;
    BookingDates bookingDates;
    String additionalNeeds;
}
