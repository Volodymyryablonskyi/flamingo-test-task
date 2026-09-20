package com.flamingo.qa.pojo.booking;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * A Restful Booker booking.
 *
 * <p>{@code @Jacksonized} is not optional: without it Jackson cannot see the Lombok builder
 * and deserialises every field to null, silently.
 *
 * <p>The naming strategy expresses the API's run-together lower case ({@code totalprice},
 * {@code additionalneeds}) once, instead of a {@code @JsonProperty} on every field.
 */
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
