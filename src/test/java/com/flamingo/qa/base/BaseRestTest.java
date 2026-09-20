package com.flamingo.qa.base;

import com.flamingo.qa.clients.BookingApiClient;
import com.flamingo.qa.data.BookingDataGenerator;
import com.flamingo.qa.extensions.RequiresService;
import com.flamingo.qa.extensions.SystemUnderTest;
import com.flamingo.qa.http.response.StatusCode;
import com.flamingo.qa.pojo.booking.Booking;
import com.flamingo.qa.pojo.booking.BookingResponse;
import com.flamingo.qa.util.CustomLogger;
import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import org.junit.jupiter.api.AfterEach;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;

/**
 * Everything a Restful Booker test needs: the clients, the dependency on the service, and
 * arranging a booking that is guaranteed to be cleaned up.
 *
 * <p>Test isolation is not optional here - classes run in parallel against a shared,
 * publicly writable service that resets itself periodically, so nothing may depend on data
 * another test left behind.
 */
@Epic("REST API - Restful Booker")
@RequiresService(SystemUnderTest.RESTFUL_BOOKER)
public abstract class BaseRestTest extends BaseApiTest {

    private static final CustomLogger log = CustomLogger.getLogger(BaseRestTest.class);

    /** 405 is what the API answers for a DELETE of something that is no longer there. */
    private static final Set<StatusCode> ACCEPTABLE_CLEANUP_OUTCOMES = Set.of(
            StatusCode.STATUS_201_CREATED,
            StatusCode.STATUS_404_NOT_FOUND,
            StatusCode.STATUS_405_METHOD_NOT_ALLOWED);

    protected final BookingApiClient bookingClient = BookingApiClient.authenticated();

    /**
     * The same calls without a token. Creates and searches genuinely need no auth; for
     * writes it is what makes "this is supposed to be rejected" visible at the call site.
     */
    protected final BookingApiClient unauthenticatedBookingClient = BookingApiClient.unauthenticated();

    /** JUnit builds a fresh test instance per method, so this is per-test state. */
    private final Deque<Integer> createdBookingIds = new ArrayDeque<>();

    @Step("Given an existing booking")
    protected BookingResponse anExistingBooking() {
        return anExistingBooking(BookingDataGenerator.validBooking());
    }

    @Step("Given an existing booking for {booking.firstname} {booking.lastname}")
    protected BookingResponse anExistingBooking(Booking booking) {
        BookingResponse created = unauthenticatedBookingClient.create(booking)
                .verify().hasStatusCode(StatusCode.STATUS_200_OK)
                .and().asPojo(BookingResponse.class);

        createdBookingIds.push(created.getBookingId());
        return created;
    }

    /** Stops tracking an id the test deleted itself. */
    protected void forget(int bookingId) {
        createdBookingIds.remove(bookingId);
    }

    /**
     * Forgiving on purpose: a booking that is already gone is a successful teardown, and a
     * teardown that throws would replace the real failure with its own.
     */
    @AfterEach
    @Step("Remove bookings created by this test")
    void removeBookingsCreatedByThisTest() {
        while (!createdBookingIds.isEmpty()) {
            int id = createdBookingIds.pop();
            try {
                int status = bookingClient.delete(id).statusCodeValue();
                if (StatusCode.of(status).filter(ACCEPTABLE_CLEANUP_OUTCOMES::contains).isEmpty()) {
                    log.warn("Cleanup of booking {} returned {}", id, StatusCode.describe(status));
                }
            } catch (RuntimeException e) {
                log.warn("Cleanup of booking {} failed: {}", id, e.toString());
            }
        }
    }
}
