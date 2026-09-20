package com.flamingo.qa.clients;

import com.flamingo.qa.config.RestAssuredConfigurator;
import com.flamingo.qa.endpoints.BookingEndpoints;
import com.flamingo.qa.http.request.HttpMethod;
import com.flamingo.qa.http.response.ResponseWrapper;
import com.flamingo.qa.pojo.booking.Booking;
import io.qameta.allure.Step;
import io.restassured.specification.RequestSpecification;

import java.util.Map;
import java.util.function.Supplier;

public class BookingApiClient extends BaseApiClient<BookingEndpoints> {

    private BookingApiClient(Supplier<RequestSpecification> spec, boolean reauthenticatesOn403) {
        super(spec, new BookingEndpoints(), reauthenticatesOn403);
    }

    public static BookingApiClient unauthenticated() {
        return new BookingApiClient(RestAssuredConfigurator::restSpec, false);
    }

    public static BookingApiClient authenticated() {
        return new BookingApiClient(
                () -> RestAssuredConfigurator.authenticatedRestSpec(TokenProvider.token()), true);
    }

    @Step("Create a booking for {booking.firstname} {booking.lastname}")
    public ResponseWrapper create(Booking booking) {
        return request(HttpMethod.POST, endpoints.getCreateUri(), booking);
    }

    @Step("Create a booking from a raw payload")
    public ResponseWrapper createRaw(Map<String, Object> payload) {
        return request(HttpMethod.POST, endpoints.getCreateUri(), payload);
    }

    @Step("Fetch booking {bookingId}")
    public ResponseWrapper getById(int bookingId) {
        return request(HttpMethod.GET, endpoints.getByIdUri(bookingId));
    }

    @Step("Replace booking {bookingId}")
    public ResponseWrapper update(int bookingId, Booking booking) {
        return request(HttpMethod.PUT, endpoints.getByIdUri(bookingId), booking);
    }

    @Step("Patch booking {bookingId} with {changes}")
    public ResponseWrapper partiallyUpdate(int bookingId, Map<String, Object> changes) {
        return request(HttpMethod.PATCH, endpoints.getByIdUri(bookingId), changes);
    }

    @Step("Delete booking {bookingId}")
    public ResponseWrapper delete(int bookingId) {
        return request(HttpMethod.DELETE, endpoints.getByIdUri(bookingId));
    }

    @Step("Find booking ids for guest '{firstname} {lastname}'")
    public ResponseWrapper findByGuestName(String firstname, String lastname) {
        return request(HttpMethod.GET, endpoints.getSearchUri(), null,
                Map.of("firstname", firstname, "lastname", lastname));
    }
}
