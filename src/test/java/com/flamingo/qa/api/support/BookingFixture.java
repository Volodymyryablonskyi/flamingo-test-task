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
 * Creates bookings and guarantees they are removed afterwards.
 *
 * <p>Test isolation on a shared, mutable, publicly writable service is not optional: tests
 * run in parallel and the service is periodically reset, so nothing may depend on data a
 * previous test left behind or on records that happen to exist.
 *
 * <p>Cleanup is deliberately forgiving. A booking that is already gone - because Restful
 * Booker reset itself mid-run, which the brief warns about - is a successful outcome for a
 * teardown, not a failure to report. Anything else is logged rather than thrown, because a
 * teardown that throws replaces the real failure with its own.
 */
public class BookingFixture {

    private static final Logger log = LoggerFactory.getLogger(BookingFixture.class);

    private final BookingClient client;
    private final Deque<Integer> createdIds = new ArrayDeque<>();

    public BookingFixture(BookingClient client) {
        this.client = client;
    }

    /** Creates a booking with generated data and registers it for cleanup. */
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

    /**
     * Stops tracking an id, for a test that deletes the booking itself - re-deleting would
     * just add a pointless call to a public service.
     */
    public void forget(int bookingId) {
        createdIds.remove(bookingId);
    }

    /** Removes everything this fixture created. Safe to call when nothing was created. */
    @Step("Remove bookings created by this test")
    public void cleanUp() {
        while (!createdIds.isEmpty()) {
            int id = createdIds.pop();
            try {
                int status = client.deleteReturningResponse(id).statusCode();
                if (status != 201 && status != 404 && status != 405) {
                    log.warn("Cleanup of booking {} returned HTTP {}", id, status);
                }
            } catch (RuntimeException e) {
                log.warn("Cleanup of booking {} failed: {}", id, e.toString());
            }
        }
    }
}
