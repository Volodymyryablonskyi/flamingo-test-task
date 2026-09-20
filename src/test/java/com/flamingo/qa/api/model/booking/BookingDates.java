package com.flamingo.qa.api.model.booking;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDate;

/**
 * The check-in / check-out pair. Modelled as {@link LocalDate} rather than {@code String}
 * so a test can say {@code checkIn.plusDays(3)} instead of formatting dates by hand.
 */
@Value
@Builder
@Jacksonized
@JsonNaming(PropertyNamingStrategies.LowerCaseStrategy.class)
public class BookingDates {

    LocalDate checkin;
    LocalDate checkout;
}
