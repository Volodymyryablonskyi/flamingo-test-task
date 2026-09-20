package com.flamingo.qa.pojo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.flamingo.qa.pojo.booking.Booking;
import com.flamingo.qa.pojo.booking.BookingDates;
import com.flamingo.qa.pojo.booking.BookingResponse;
import com.flamingo.qa.util.Json;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the mapping between the Lombok models and the JSON Restful Booker actually speaks.
 * Worth its own test because the failure mode is silent: a builder Jackson cannot see
 * yields an object with every field null, and nothing throws.
 *
 * <p>The literal below is a real response body, captured from the live API.
 */
@Tag("unit")
@DisplayName("Booking JSON mapping")
class BookingSerializationTest {

    private static final String LIVE_RESPONSE_BODY = """
            {
              "firstname": "Probe",
              "lastname": "Verify",
              "totalprice": 123,
              "depositpaid": true,
              "bookingdates": { "checkin": "2026-10-01", "checkout": "2026-10-05" },
              "additionalneeds": "Breakfast"
            }""";

    @Test
    @DisplayName("deserialises every field of a real response body")
    void deserialisesALiveResponseBody() throws JsonProcessingException {
        Booking booking = Json.mapper().readValue(LIVE_RESPONSE_BODY, Booking.class);

        assertThat(booking.getFirstname()).isEqualTo("Probe");
        assertThat(booking.getLastname()).isEqualTo("Verify");
        assertThat(booking.getTotalPrice()).isEqualTo(123);
        assertThat(booking.getDepositPaid()).isTrue();
        assertThat(booking.getAdditionalNeeds()).isEqualTo("Breakfast");
        assertThat(booking.getBookingDates().getCheckin()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(booking.getBookingDates().getCheckout()).isEqualTo(LocalDate.of(2026, 10, 5));
    }

    @Test
    @DisplayName("serialises back to the run-together lower-case names the API expects")
    void serialisesToTheApiFieldNames() throws JsonProcessingException {
        Booking booking = Booking.builder()
                .firstname("Probe")
                .lastname("Verify")
                .totalPrice(123)
                .depositPaid(true)
                .bookingDates(BookingDates.builder()
                        .checkin(LocalDate.of(2026, 10, 1))
                        .checkout(LocalDate.of(2026, 10, 5))
                        .build())
                .additionalNeeds("Breakfast")
                .build();

        String json = Json.mapper().writeValueAsString(booking);

        // Compared as trees: key order and whitespace are not contract.
        assertThat(Json.mapper().readTree(json))
                .isEqualTo(Json.mapper().readTree(LIVE_RESPONSE_BODY));
    }

    @Test
    @DisplayName("unwraps the id-plus-booking envelope that only POST /booking returns")
    void deserialisesTheCreationEnvelope() throws JsonProcessingException {
        String body = "{\"bookingid\":388,\"booking\":" + LIVE_RESPONSE_BODY + "}";

        BookingResponse response = Json.mapper().readValue(body, BookingResponse.class);

        assertThat(response.getBookingId()).isEqualTo(388);
        assertThat(response.getBooking().getFirstname()).isEqualTo("Probe");
    }
}
