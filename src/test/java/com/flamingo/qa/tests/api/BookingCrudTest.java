package com.flamingo.qa.tests.api;

import com.flamingo.qa.api.model.booking.Booking;
import com.flamingo.qa.api.model.booking.BookingDates;
import com.flamingo.qa.api.model.booking.BookingResponse;
import com.flamingo.qa.core.util.TestDataFactory;
import com.flamingo.qa.tests.BaseRestTest;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

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
        Booking request = TestDataFactory.aBooking();

        BookingResponse created = bookings.anExistingBooking(request);

        assertThat(created.getBookingId()).isNotNull().isPositive();
        assertThat(created.getBooking()).usingRecursiveComparison().isEqualTo(request);
    }

    @Test
    @Tag("smoke")
    @Story("A created booking can be read back")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("retrieves a created booking by id")
    void shouldRetrieveCreatedBookingById() {
        BookingResponse created = bookings.anExistingBooking();

        Booking fetched = bookingClient.getById(created.getBookingId());

        assertThat(fetched).usingRecursiveComparison().isEqualTo(created.getBooking());
    }

    @Test
    @Story("A booking can be replaced")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("replaces every field of an existing booking")
    void shouldUpdateAllFieldsOfExistingBooking() {
        BookingResponse created = bookings.anExistingBooking();
        Booking replacement = Booking.builder()
                .firstname("Replaced")
                .lastname(TestDataFactory.uniqueLastName())
                .totalPrice(4_242)
                .depositPaid(!created.getBooking().getDepositPaid())
                .bookingDates(BookingDates.builder()
                        .checkin(LocalDate.of(2027, 3, 1))
                        .checkout(LocalDate.of(2027, 3, 8))
                        .build())
                .additionalNeeds("Airport transfer")
                .build();

        Booking updated = bookingClient.update(created.getBookingId(), replacement);

        assertThat(updated).usingRecursiveComparison().isEqualTo(replacement);
        // The response body is the server's word for it; re-reading is the proof.
        assertThat(bookingClient.getById(created.getBookingId()))
                .usingRecursiveComparison()
                .isEqualTo(replacement);
    }

    @Test
    @Story("Writes are protected")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("rejects an update sent without an auth token")
    void shouldRejectUpdateWithoutAuthToken() {
        BookingResponse created = bookings.anExistingBooking();

        Response response = bookingClient.updateWithoutAuthentication(
                created.getBookingId(), TestDataFactory.aBooking());

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.asString()).isEqualTo("Forbidden");
        assertThat(bookingClient.getById(created.getBookingId()))
                .as("a rejected update must not have changed anything")
                .usingRecursiveComparison()
                .isEqualTo(created.getBooking());
    }

    @Test
    @Story("A booking can be partially updated")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("patches named fields and leaves the rest untouched")
    void shouldPartiallyUpdateBooking() {
        BookingResponse created = bookings.anExistingBooking();
        Booking original = created.getBooking();

        Booking patched = bookingClient.partiallyUpdate(created.getBookingId(),
                Map.of("firstname", "Patched", "totalprice", 777));

        // The interesting half of a PATCH is what it did not change, so assert the whole object.
        assertThat(patched).usingRecursiveComparison().isEqualTo(original.toBuilder()
                .firstname("Patched")
                .totalPrice(777)
                .build());
    }

    @Test
    @Tag("smoke")
    @Story("A booking can be deleted")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("deletes a booking, after which it is really gone")
    void shouldDeleteBookingAndReturn404OnSubsequentGet() {
        BookingResponse created = bookings.anExistingBooking();
        int id = created.getBookingId();

        bookingClient.delete(id);
        bookings.forget(id);

        // A delete that reported success without removing anything would otherwise pass.
        Response afterDeletion = bookingClient.getByIdReturningResponse(id);
        assertThat(afterDeletion.statusCode()).isEqualTo(404);
        assertThat(afterDeletion.asString()).isEqualTo("Not Found");
    }

    @Test
    @Story("A booking can be created")
    @Severity(SeverityLevel.MINOR)
    @DisplayName("stores the dates it was given, unshifted by time zones")
    void shouldStoreBookingDatesWithoutTimeZoneShift() {
        LocalDate checkIn = LocalDate.now().plusDays(30);
        Booking request = TestDataFactory.aBooking().toBuilder()
                .bookingDates(BookingDates.builder()
                        .checkin(checkIn)
                        .checkout(checkIn.plusDays(7))
                        .build())
                .build();

        BookingResponse created = bookings.anExistingBooking(request);
        BookingDates stored = bookingClient.getById(created.getBookingId()).getBookingDates();

        // Compared against locally built dates, so a symmetric serialisation bug cannot hide.
        assertThat(stored.getCheckin()).isEqualTo(checkIn);
        assertThat(stored.getCheckout()).isEqualTo(checkIn.plusDays(7));
    }
}
