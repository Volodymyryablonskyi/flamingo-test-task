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
 * Everything the suite does to {@code /booking}.
 *
 * <p>Typed methods for happy paths, so tests read {@code booking.getFirstname()} with no
 * JSON paths in them; raw-{@code Response} overloads for negative paths, where the status
 * and error body are the subject and pre-validating them would assert the test away.
 *
 * <p>Authenticated calls resolve the token through {@link TokenProvider}, so no test has to
 * carry one around.
 */
public class BookingClient {

    private static final String BOOKINGS = "/booking";
    private static final String BOOKING_BY_ID = "/booking/{id}";

    /** A successful DELETE answers 201 Created, not 200 or 204. */
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
     * A map, not a model: a partly-null {@link Booking} could not express "leave totalprice
     * alone" distinctly from "set it to null".
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

    @Step("Delete booking {id} (raw response)")
    public Response deleteReturningResponse(int id) {
        return TransientFailureRetry.send(() -> given()
                .spec(RequestSpecs.authenticated(TokenProvider.token()))
                .pathParam("id", id)
                .when()
                .delete(BOOKING_BY_ID));
    }

    /** The search returns ids only - {@code [{"bookingid":1}]} - not bookings. */
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
