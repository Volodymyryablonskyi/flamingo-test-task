package com.flamingo.qa.fixtures;

import com.flamingo.qa.clients.BookingApiClient;
import com.flamingo.qa.data.BookingDataGenerator;
import com.flamingo.qa.http.response.StatusCode;
import com.flamingo.qa.pojo.booking.Booking;
import com.flamingo.qa.pojo.booking.BookingResponse;
import com.flamingo.qa.util.CustomLogger;
import io.qameta.allure.Step;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;

/**
 * Creates bookings and guarantees they are removed, so no test depends on data another left
 * behind - mandatory with parallel classes against a shared, periodically reset service.
 *
 * <p>Cleanup is forgiving: a booking that is already gone is a successful teardown, and a
 * teardown that throws would replace the real failure with its own.
 */
public class BookingFixture {

    private static final CustomLogger log = CustomLogger.getLogger(BookingFixture.class);

    /** 405 is what the API answers for a DELETE of something that is no longer there. */
    private static final Set<StatusCode> ACCEPTABLE_CLEANUP_OUTCOMES = Set.of(
            StatusCode.STATUS_201_CREATED,
            StatusCode.STATUS_404_NOT_FOUND,
            StatusCode.STATUS_405_METHOD_NOT_ALLOWED);

    private final BookingApiClient authenticatedClient = BookingApiClient.authenticated();
    private final BookingApiClient unauthenticatedClient = BookingApiClient.unauthenticated();
    private final Deque<Integer> createdIds = new ArrayDeque<>();

    @Step("Given an existing booking")
    public BookingResponse anExistingBooking() {
        return anExistingBooking(BookingDataGenerator.validBooking());
    }

    @Step("Given an existing booking for {booking.firstname} {booking.lastname}")
    public BookingResponse anExistingBooking(Booking booking) {
        BookingResponse created = unauthenticatedClient.create(booking)
                .verify().hasStatusCode(StatusCode.STATUS_200_OK)
                .and().asPojo(BookingResponse.class);

        createdIds.push(created.getBookingId());
        return created;
    }

    /** Stops tracking an id the test deleted itself. */
    public void forget(int bookingId) {
        createdIds.remove(bookingId);
    }

    @Step("Remove bookings created by this test")
    public void cleanUp() {
        while (!createdIds.isEmpty()) {
            int id = createdIds.pop();
            try {
                int status = authenticatedClient.delete(id).statusCodeValue();
                if (StatusCode.of(status).filter(ACCEPTABLE_CLEANUP_OUTCOMES::contains).isEmpty()) {
                    log.warn("Cleanup of booking {} returned {}", id, StatusCode.describe(status));
                }
            } catch (RuntimeException e) {
                log.warn("Cleanup of booking {} failed: {}", id, e.toString());
            }
        }
    }
}
