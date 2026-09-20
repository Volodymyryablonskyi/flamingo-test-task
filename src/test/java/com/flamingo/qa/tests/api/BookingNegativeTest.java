package com.flamingo.qa.tests.api;

import com.flamingo.qa.base.BaseRestTest;
import com.flamingo.qa.pojo.booking.BookingResponse;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.flamingo.qa.http.response.StatusCode.STATUS_200_OK;
import static com.flamingo.qa.http.response.StatusCode.STATUS_403_FORBIDDEN;
import static com.flamingo.qa.http.response.StatusCode.STATUS_404_NOT_FOUND;
import static com.flamingo.qa.http.response.StatusCode.STATUS_500_INTERNAL_SERVER_ERROR;

/**
 * What the API does when it is asked for something it cannot do. Each expectation is the
 * contract as measured against the live service, not the contract it ought to have.
 */
@Feature("Booking error handling")
@DisplayName("Booking negative paths")
class BookingNegativeTest extends BaseRestTest {

    /** Far above any id the service allocates, so it cannot collide after a reset. */
    private static final int NON_EXISTENT_BOOKING_ID = 999_999_999;

    @Test
    @Story("A request for something that is not there is refused cleanly")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("returns 404 for a booking id that does not exist")
    void shouldReturn404ForNonExistentBookingId() {
        bookingClient.getById(NON_EXISTENT_BOOKING_ID)
                .verify()
                .hasStatusCode(STATUS_404_NOT_FOUND)
                .hasBodyEqualTo("Not Found");
    }

    @Test
    @Story("Writes are protected")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("rejects a delete sent without an auth token, leaving the booking intact")
    void shouldRejectDeleteWithoutAuthToken() {
        BookingResponse created = anExistingBooking();

        unauthenticatedBookingClient.delete(created.getBookingId())
                .verify()
                .hasStatusCode(STATUS_403_FORBIDDEN);

        // The point of the test: authorisation refused the write, it did not merely report so.
        bookingClient.getById(created.getBookingId())
                .verify()
                .hasStatusCode(STATUS_200_OK);
    }

    @Test
    @Story("A request for something that is not there is refused cleanly")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("answers 500 - not 400 - when the payload is missing required fields")
    void shouldReturnServerErrorForIncompletePayload() {
        // A defect in the API, pinned rather than wished away: a body the client got wrong
        // should be a 4xx. Asserting 400 here would fail against the service as it is, and
        // asserting nothing would let the behaviour change unnoticed.
        //
        // Note this call is sent three times: 5xx is retryable at the transport layer, so
        // the retry runs its full course before the status is asserted.
        unauthenticatedBookingClient.createRaw(Map.of("firstname", "MissingEverythingElse"))
                .verify()
                .hasStatusCode(STATUS_500_INTERNAL_SERVER_ERROR)
                .hasBodyEqualTo("Internal Server Error");
    }
}
