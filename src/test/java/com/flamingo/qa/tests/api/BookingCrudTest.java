package com.flamingo.qa.tests.api;

import com.flamingo.qa.api.model.booking.Booking;
import com.flamingo.qa.api.model.booking.BookingDates;
import com.flamingo.qa.api.model.booking.BookingResponse;
import com.flamingo.qa.core.util.TestDataFactory;
import com.flamingo.qa.tests.BaseRestTest;
import io.qameta.allure.Description;
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

/**
 * The CRUD lifecycle of a booking.
 *
 * <p>Each test creates the data it needs through the inherited fixture and nothing depends
 * on another test's side effects - mandatory here, because the classes run in parallel and
 * Restful Booker resets itself periodically.
 */
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

        assertThat(created.getBookingId())
                .as("server-assigned booking id")
                .isNotNull()
                .isPositive();
        assertThat(created.getBooking())
                .as("the stored booking should match what we sent, field for field")
                .usingRecursiveComparison()
                .isEqualTo(request);
    }

    @Test
    @Tag("smoke")
    @Story("A created booking can be read back")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("retrieves a created booking by id")
    void shouldRetrieveCreatedBookingById() {
        BookingResponse created = bookings.anExistingBooking();

        Booking fetched = bookingClient.getById(created.getBookingId());

        assertThat(fetched)
                .as("GET must return exactly what POST reported as stored")
                .usingRecursiveComparison()
                .isEqualTo(created.getBooking());
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
                .as("the replacement must be persisted, not merely echoed")
                .usingRecursiveComparison()
                .isEqualTo(replacement);
    }

    @Test
    @Story("Writes are protected")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("rejects an update sent without an auth token")
    @Description("""
            The token travels as a `token` cookie. A request carrying it as an
            Authorization: Bearer header is rejected exactly like an unauthenticated one -
            which is why RequestSpecs owns that detail instead of each test.""")
    void shouldRejectUpdateWithoutAuthToken() {
        BookingResponse created = bookings.anExistingBooking();
        Booking attempted = TestDataFactory.aBooking();

        Response response = bookingClient.updateWithoutAuthentication(created.getBookingId(), attempted);

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

        // The interesting half of a PATCH is what it did NOT change, so assert the whole
        // object: the fields we sent, and every field we did not.
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
    @Description("""
            A successful DELETE answers 201 Created - an odd choice by the API, asserted as
            measured. The follow-up GET is the part that matters: a delete that reports
            success without removing anything would otherwise pass.""")
    void shouldDeleteBookingAndReturn404OnSubsequentGet() {
        BookingResponse created = bookings.anExistingBooking();
        int id = created.getBookingId();

        bookingClient.delete(id);
        bookings.forget(id);

        Response afterDeletion = bookingClient.getByIdReturningResponse(id);
        assertThat(afterDeletion.statusCode()).isEqualTo(404);
        assertThat(afterDeletion.asString()).isEqualTo("Not Found");
    }

    @Test
    @Story("A booking can be created")
    @Severity(SeverityLevel.MINOR)
    @DisplayName("stores the dates it was given, unshifted by time zones")
    @Description("""
            A date sent as 2026-10-01 coming back as 2026-09-30 is the classic serialisation
            bug on a suite whose CI runner sits in a different zone from the developer.""")
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

        assertThat(stored.getCheckin()).isEqualTo(checkIn);
        assertThat(stored.getCheckout()).isEqualTo(checkIn.plusDays(7));
    }
}
