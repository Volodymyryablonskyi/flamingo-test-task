package com.flamingo.qa.base;

import com.flamingo.qa.clients.BookingApiClient;
import com.flamingo.qa.extensions.RequiresService;
import com.flamingo.qa.extensions.SystemUnderTest;
import com.flamingo.qa.fixtures.BookingFixture;
import io.qameta.allure.Epic;
import org.junit.jupiter.api.AfterEach;

/**
 * Wires up what every Restful Booker test needs: the clients, the fixture, the dependency
 * on the service, and a teardown that cannot be forgotten.
 */
@Epic("REST API - Restful Booker")
@RequiresService(SystemUnderTest.RESTFUL_BOOKER)
public abstract class BaseRestTest extends BaseApiTest {

    protected final BookingApiClient bookingClient = BookingApiClient.authenticated();

    /** For the negative tests: same calls, no token, so the rejection is the subject. */
    protected final BookingApiClient anonymousBookingClient = BookingApiClient.unauthenticated();

    /** JUnit builds a fresh test instance per method, so this is per-test state. */
    protected final BookingFixture bookings = new BookingFixture();

    @AfterEach
    void removeBookingsCreatedByThisTest() {
        bookings.cleanUp();
    }
}
