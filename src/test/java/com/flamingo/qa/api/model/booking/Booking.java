package com.flamingo.qa.api.model.booking;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * A Restful Booker booking.
 *
 * <p>{@code @Value @Builder} gives immutability and readable construction;
 * <strong>{@code @Jacksonized} is not optional</strong> - without it Jackson cannot see the
 * Lombok builder, so deserialisation silently yields an object with every field null and
 * the failure surfaces as a confusing assertion mismatch rather than a mapping error.
 *
 * <p>The API names its fields in run-together lower case ({@code totalprice},
 * {@code additionalneeds}). Rather than scatter {@code @JsonProperty} over every field, one
 * naming strategy states that convention once and the Java side keeps idiomatic camelCase.
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
