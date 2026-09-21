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

@Epic("REST API - Restful Booker")
@RequiresService(SystemUnderTest.RESTFUL_BOOKER)
public abstract class BaseRestTest extends BaseApiTest {

    private static final CustomLogger log = CustomLogger.getLogger(BaseRestTest.class);

    private static final Set<StatusCode> ACCEPTABLE_CLEANUP_OUTCOMES = Set.of(
            StatusCode.STATUS_201_CREATED,
            StatusCode.STATUS_404_NOT_FOUND,
            StatusCode.STATUS_405_METHOD_NOT_ALLOWED);

    protected final BookingApiClient bookingClient = BookingApiClient.authenticated();

    protected final BookingApiClient unauthenticatedBookingClient = BookingApiClient.unauthenticated();

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

        createdBookingIds.push(created.bookingId());
        return created;
    }

    protected void forget(int bookingId) {
        createdBookingIds.remove(bookingId);
    }

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
