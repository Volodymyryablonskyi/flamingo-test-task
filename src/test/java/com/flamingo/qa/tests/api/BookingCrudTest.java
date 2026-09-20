package com.flamingo.qa.tests.api;

import com.flamingo.qa.base.BaseRestTest;
import com.flamingo.qa.data.BookingDataGenerator;
import com.flamingo.qa.pojo.booking.Booking;
import com.flamingo.qa.pojo.booking.BookingDates;
import com.flamingo.qa.pojo.booking.BookingResponse;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static com.flamingo.qa.http.response.StatusCode.STATUS_200_OK;
import static com.flamingo.qa.http.response.StatusCode.STATUS_201_CREATED;
import static com.flamingo.qa.http.response.StatusCode.STATUS_403_FORBIDDEN;
import static com.flamingo.qa.http.response.StatusCode.STATUS_404_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;

@Feature("Booking CRUD")
@DisplayName("Booking lifecycle")
class BookingCrudTest extends BaseRestTest {

    @Test
    @Tag("smoke")
    @Story("A booking can be created")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("creates a booking and echoes every field back")
    void shouldCreateBookingWithGeneratedData() {
        Booking request = BookingDataGenerator.validBooking();

        BookingResponse created = anExistingBooking(request);

        assertThat(created.getBookingId()).isNotNull().isPositive();
        assertThat(created.getBooking()).usingRecursiveComparison().isEqualTo(request);
    }

    @Test
    @Tag("smoke")
    @Story("A created booking can be read back")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("retrieves a created booking by id")
    void shouldRetrieveCreatedBookingById() {
        BookingResponse created = anExistingBooking();

        bookingClient.getById(created.getBookingId())
                .verify()
                .hasStatusCode(STATUS_200_OK)
                .hasBodyEqualTo(Booking.class, created.getBooking());
    }

    @Test
    @Story("A booking can be replaced")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("replaces every field of an existing booking")
    void shouldUpdateAllFieldsOfExistingBooking() {
        BookingResponse created = anExistingBooking();
        Booking replacement = Booking.builder()
                .firstname("Replaced")
                .lastname(BookingDataGenerator.uniqueLastName())
                .totalPrice(4_242)
                .depositPaid(!created.getBooking().getDepositPaid())
                .bookingDates(BookingDates.builder()
                        .checkin(LocalDate.of(2027, 3, 1))
                        .checkout(LocalDate.of(2027, 3, 8))
                        .build())
                .additionalNeeds("Airport transfer")
                .build();

        bookingClient.update(created.getBookingId(), replacement)
                .verify()
                .hasStatusCode(STATUS_200_OK)
                .hasBodyEqualTo(Booking.class, replacement);

        bookingClient.getById(created.getBookingId())
                .verify()
                .hasStatusCode(STATUS_200_OK)
                .hasBodyEqualTo(Booking.class, replacement);
    }

    @Test
    @Story("Writes are protected")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("rejects an update sent without an auth token")
    void shouldRejectUpdateWithoutAuthToken() {
        BookingResponse created = anExistingBooking();

        unauthenticatedBookingClient.update(created.getBookingId(), BookingDataGenerator.validBooking())
                .verify()
                .hasStatusCode(STATUS_403_FORBIDDEN)
                .hasBodyEqualTo("Forbidden");

        bookingClient.getById(created.getBookingId())
                .verify()
                .hasStatusCode(STATUS_200_OK)
                .hasBodyEqualTo(Booking.class, created.getBooking());
    }

    @Test
    @Story("A booking can be partially updated")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("patches named fields and leaves the rest untouched")
    void shouldPartiallyUpdateBooking() {
        BookingResponse created = anExistingBooking();
        Booking expected = created.getBooking().toBuilder()
                .firstname("Patched")
                .totalPrice(777)
                .build();

        bookingClient.partiallyUpdate(created.getBookingId(),
                        Map.of("firstname", "Patched", "totalprice", 777))
                .verify()
                .hasStatusCode(STATUS_200_OK)
                .hasBodyEqualTo(Booking.class, expected);
    }

    @Test
    @Tag("smoke")
    @Story("A booking can be deleted")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("deletes a booking, after which it is really gone")
    void shouldDeleteBookingAndReturn404OnSubsequentGet() {
        BookingResponse created = anExistingBooking();
        int id = created.getBookingId();

        bookingClient.delete(id).verify().hasStatusCode(STATUS_201_CREATED);
        forget(id);

        bookingClient.getById(id)
                .verify()
                .hasStatusCode(STATUS_404_NOT_FOUND)
                .hasBodyEqualTo("Not Found");
    }

    @Test
    @Story("A booking can be created")
    @Severity(SeverityLevel.MINOR)
    @DisplayName("stores the dates it was given, unshifted by time zones")
    void shouldStoreBookingDatesWithoutTimeZoneShift() {
        LocalDate checkIn = LocalDate.now().plusDays(30);
        LocalDate checkOut = checkIn.plusDays(7);

        BookingResponse created = anExistingBooking(
                BookingDataGenerator.bookingStaying(checkIn, checkOut));

        BookingDates stored = bookingClient.getById(created.getBookingId())
                .verify().hasStatusCode(STATUS_200_OK)
                .and().asPojo(Booking.class)
                .getBookingDates();

        assertThat(stored.getCheckin()).isEqualTo(checkIn);
        assertThat(stored.getCheckout()).isEqualTo(checkOut);
    }
}
