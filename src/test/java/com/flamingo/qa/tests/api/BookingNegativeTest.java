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

@Feature("Booking error handling")
@DisplayName("Booking negative paths")
class BookingNegativeTest extends BaseRestTest {

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

        unauthenticatedBookingClient.delete(created.bookingId())
                .verify()
                .hasStatusCode(STATUS_403_FORBIDDEN);

        bookingClient.getById(created.bookingId())
                .verify()
                .hasStatusCode(STATUS_200_OK);
    }

    @Test
    @Story("A request for something that is not there is refused cleanly")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("answers 500 - not 400 - when the payload is missing required fields")
    void shouldReturnServerErrorForIncompletePayload() {
        unauthenticatedBookingClient.createRaw(Map.of("firstname", "MissingEverythingElse"))
                .verify()
                .hasStatusCode(STATUS_500_INTERNAL_SERVER_ERROR)
                .hasBodyEqualTo("Internal Server Error");
    }
}
