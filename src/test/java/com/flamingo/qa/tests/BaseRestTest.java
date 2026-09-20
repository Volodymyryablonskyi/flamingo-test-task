package com.flamingo.qa.tests;

import com.flamingo.qa.api.client.BookingClient;
import com.flamingo.qa.api.support.BookingFixture;
import com.flamingo.qa.core.extension.RequiresService;
import com.flamingo.qa.core.extension.SystemUnderTest;
import io.qameta.allure.Epic;
import org.junit.jupiter.api.AfterEach;

/**
 * Base class for the Restful Booker REST tests.
 *
 * <p>Two jobs, both about things every REST test needs and none should restate:
 *
 * <ul>
 *   <li>declaring the dependency on Restful Booker, so a cold-started or reset Heroku dyno
 *       skips these classes with a reason instead of failing them;</li>
 *   <li>owning the {@link BookingFixture} and guaranteeing teardown, so no test can leave
 *       data behind on a public service even when it fails part-way through.</li>
 * </ul>
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
