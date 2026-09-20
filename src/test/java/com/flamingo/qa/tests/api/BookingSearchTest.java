package com.flamingo.qa.tests.api;

import com.flamingo.qa.base.BaseRestTest;
import com.flamingo.qa.data.BookingDataGenerator;
import com.flamingo.qa.data.GuestNameCase;
import com.flamingo.qa.pojo.booking.Booking;
import com.flamingo.qa.pojo.booking.BookingResponse;
import com.flamingo.qa.util.ResourceReader;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;

import static com.flamingo.qa.http.response.StatusCode.STATUS_200_OK;
import static org.assertj.core.api.Assertions.assertThat;

@Feature("Booking search")
@DisplayName("GET /booking?firstname=&lastname=")
class BookingSearchTest extends BaseRestTest {

    private static final String CASES = "testdata/guest-name-cases.json";

    static List<GuestNameCase> guestNameCases() {
        return ResourceReader.readList(CASES, GuestNameCase.class);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("guestNameCases")
    @Story("A booking can be found by the guest's name")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("finds a booking by guest name")
    void shouldFilterBookingIdsByGuestName(GuestNameCase guest) {
        // The surname carries a run-unique suffix: this service is shared and the search
        // spans everyone's data, so a bare "Ada Lovelace" would match strangers' records.
        String lastname = guest.lastname() + "-" + BookingDataGenerator.uniqueLastName();
        Booking booking = BookingDataGenerator.validBooking().toBuilder()
                .firstname(guest.firstname())
                .lastname(lastname)
                .build();
        BookingResponse created = anExistingBooking(booking);

        List<Integer> ids = unauthenticatedBookingClient.findByGuestName(guest.firstname(), lastname)
                .verify().hasStatusCode(STATUS_200_OK)
                .and().asListOfField("bookingid", Integer.class);

        assertThat(ids)
                .as("search for %s %s", guest.firstname(), lastname)
                .containsExactly(created.getBookingId());
    }

    @Test
    @Story("A booking can be found by the guest's name")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("returns an empty list when nobody matches")
    void shouldReturnNoIdsForUnknownGuest() {
        List<Integer> ids = unauthenticatedBookingClient
                .findByGuestName("Nobody", BookingDataGenerator.uniqueLastName())
                .verify().hasStatusCode(STATUS_200_OK)
                .and().asListOfField("bookingid", Integer.class);

        // An empty array, not a 404: no match is a valid answer to a valid query.
        assertThat(ids).isEmpty();
    }
}
