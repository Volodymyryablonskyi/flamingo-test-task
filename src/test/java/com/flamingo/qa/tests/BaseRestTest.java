package com.flamingo.qa.tests;

import com.flamingo.qa.api.client.BookingClient;
import com.flamingo.qa.api.support.BookingFixture;
import com.flamingo.qa.core.extension.RequiresService;
import com.flamingo.qa.core.extension.SystemUnderTest;
import io.qameta.allure.Epic;
import org.junit.jupiter.api.AfterEach;

/**
 * Declares the dependency on Restful Booker once, and guarantees teardown so no test can
 * leave data on a public service even when it fails part-way through.
 */
@Epic("REST API - Restful Booker")
@RequiresService(SystemUnderTest.RESTFUL_BOOKER)
public abstract class BaseRestTest extends BaseApiTest {

    protected final BookingClient bookingClient = new BookingClient();

    /** JUnit builds a fresh test instance per method, so this is per-test state. */
    protected final BookingFixture bookings = new BookingFixture(bookingClient);

    @AfterEach
    void removeBookingsCreatedByThisTest() {
        bookings.cleanUp();
    }
}
