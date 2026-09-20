package com.flamingo.qa.api.support;

import com.flamingo.qa.api.client.BookingClient;
import com.flamingo.qa.api.model.booking.Booking;
import com.flamingo.qa.api.model.booking.BookingResponse;
import com.flamingo.qa.core.util.TestDataFactory;
import io.qameta.allure.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Creates bookings and guarantees they are removed, so no test depends on data another left
 * behind - mandatory with parallel classes against a shared, periodically reset service.
 *
 * <p>Cleanup is forgiving: a booking that is already gone is a successful teardown, and a
 * teardown that throws would replace the real failure with its own.
 */
public class BookingFixture {

    private static final Logger log = LoggerFactory.getLogger(BookingFixture.class);

    private static final int DELETED = 201;
    private static final int ALREADY_GONE = 404;
    private static final int NOT_ALLOWED_BECAUSE_GONE = 405;

    private final BookingClient client;
    private final Deque<Integer> createdIds = new ArrayDeque<>();

    public BookingFixture(BookingClient client) {
        this.client = client;
    }

    @Step("Given an existing booking")
    public BookingResponse anExistingBooking() {
        return anExistingBooking(TestDataFactory.aBooking());
    }

    @Step("Given an existing booking for {booking.firstname} {booking.lastname}")
    public BookingResponse anExistingBooking(Booking booking) {
        BookingResponse created = client.create(booking);
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
                int status = client.deleteReturningResponse(id).statusCode();
                if (status != DELETED && status != ALREADY_GONE && status != NOT_ALLOWED_BECAUSE_GONE) {
                    log.warn("Cleanup of booking {} returned HTTP {}", id, status);
                }
            } catch (RuntimeException e) {
                log.warn("Cleanup of booking {} failed: {}", id, e.toString());
            }
        }
    }
}
