package com.flamingo.qa.pojo.booking;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;

import java.time.LocalDate;

@Builder
@JsonNaming(PropertyNamingStrategies.LowerCaseStrategy.class)
public record BookingDates(LocalDate checkin, LocalDate checkout) {
}
