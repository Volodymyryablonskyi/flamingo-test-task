package com.flamingo.qa.api.client;

import com.flamingo.qa.api.model.booking.Booking;
import com.flamingo.qa.api.model.booking.BookingResponse;
import com.flamingo.qa.api.spec.RequestSpecs;
import com.flamingo.qa.api.spec.ResponseSpecs;
import com.flamingo.qa.api.support.TransientFailureRetry;
import io.qameta.allure.Step;
import io.restassured.response.Response;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * Everything this suite does to {@code /booking}.
 *
 * <p>Two shapes of method, on purpose:
 *
 * <ul>
 *   <li><strong>Typed</strong> ({@link #create}, {@link #getById}) - the happy paths. They
 *       assert the transport-level expectations through a {@code ResponseSpecification} and
 *       hand back a model, so a test reads
 *       {@code assertThat(booking.getFirstname()).isEqualTo(...)} with no JSON paths in it.</li>
 *   <li><strong>Raw</strong> ({@link #getByIdReturningResponse}, {@link #updateWithoutAuthentication})
 *       - the negative paths, where the status code and the error body <em>are</em> the
 *       subject and pre-validating them would assert the thing under test away.</li>
 * </ul>
 *
 * <p>Authenticated calls resolve the token themselves through {@link TokenProvider}, so no
 * test has to carry one around.
 *
 * <p>Every call goes out through {@link TransientFailureRetry}, which sits below the
 * response-spec validation: a retried request never re-runs an assertion.
 */
public class BookingClient {

    private static final String BOOKINGS = "/booking";
    private static final String BOOKING_BY_ID = "/booking/{id}";

    // Restful Booker answers a successful DELETE with 201 Created, not 200 or 204.
    private static final int DELETED = 201;

    @Step("Create a booking for {booking.firstname} {booking.lastname}")
    public BookingResponse create(Booking booking) {
        return TransientFailureRetry.send(() -> given()
                .spec(RequestSpecs.unauthenticated())
                .body(booking)
                .when()
                .post(BOOKINGS))
                .then()
                .spec(ResponseSpecs.okJson())
                .extract()
                .as(BookingResponse.class);
    }

    @Step("Fetch booking {id}")
    public Booking getById(int id) {
        return TransientFailureRetry.send(() -> given()
                .spec(RequestSpecs.unauthenticated())
                .pathParam("id", id)
                .when()
                .get(BOOKING_BY_ID))
                .then()
                .spec(ResponseSpecs.okJson())
                .extract()
                .as(Booking.class);
    }

    /** Unvalidated, for tests that expect the fetch to fail. */
    @Step("Fetch booking {id} (raw response)")
    public Response getByIdReturningResponse(int id) {
        return TransientFailureRetry.send(() -> given()
                .spec(RequestSpecs.unauthenticated())
                .pathParam("id", id)
                .when()
                .get(BOOKING_BY_ID));
    }

    @Step("Replace booking {id}")
    public Booking update(int id, Booking booking) {
        return TransientFailureRetry.send(() -> given()
                .spec(RequestSpecs.authenticated(TokenProvider.token()))
                .pathParam("id", id)
                .body(booking)
                .when()
                .put(BOOKING_BY_ID))
                .then()
                .spec(ResponseSpecs.okJson())
                .extract()
                .as(Booking.class);
    }

    /** Deliberately omits the token, to prove the endpoint is actually protected. */
    @Step("Replace booking {id} without authenticating")
    public Response updateWithoutAuthentication(int id, Booking booking) {
        return TransientFailureRetry.send(() -> given()
                .spec(RequestSpecs.unauthenticated())
                .pathParam("id", id)
                .body(booking)
                .when()
                .put(BOOKING_BY_ID));
    }

    /**
     * A map rather than a model: a PATCH body is by definition a subset, and a partly-null
     * {@link Booking} could not express "leave {@code totalprice} alone" distinctly from
     * "set it to null".
     */
    @Step("Patch booking {id} with {changes}")
    public Booking partiallyUpdate(int id, Map<String, Object> changes) {
        return TransientFailureRetry.send(() -> given()
                .spec(RequestSpecs.authenticated(TokenProvider.token()))
                .pathParam("id", id)
                .body(changes)
                .when()
                .patch(BOOKING_BY_ID))
                .then()
                .spec(ResponseSpecs.okJson())
                .extract()
                .as(Booking.class);
    }

    @Step("Delete booking {id}")
    public void delete(int id) {
        TransientFailureRetry.send(() -> given()
                .spec(RequestSpecs.authenticated(TokenProvider.token()))
                .pathParam("id", id)
                .when()
                .delete(BOOKING_BY_ID))
                .then()
                .spec(ResponseSpecs.status(DELETED));
    }

    /** Unvalidated delete, used by cleanup: a booking may already be gone after a service reset. */
    @Step("Delete booking {id} (raw response)")
    public Response deleteReturningResponse(int id) {
        return TransientFailureRetry.send(() -> given()
                .spec(RequestSpecs.authenticated(TokenProvider.token()))
                .pathParam("id", id)
                .when()
                .delete(BOOKING_BY_ID));
    }

    /**
     * {@code GET /booking?firstname=&lastname=} returns only ids - {@code [{"bookingid":1}]} -
     * so the caller gets ids, not bookings, and has to fetch what it wants to inspect.
     */
    @Step("Find booking ids for guest \"{firstname} {lastname}\"")
    public List<Integer> findIdsByGuestName(String firstname, String lastname) {
        return TransientFailureRetry.send(() -> given()
                .spec(RequestSpecs.unauthenticated())
                .queryParam("firstname", firstname)
                .queryParam("lastname", lastname)
                .when()
                .get(BOOKINGS))
                .then()
                .spec(ResponseSpecs.okJson())
                .extract()
                .jsonPath()
                .getList("bookingid", Integer.class);
    }
}
