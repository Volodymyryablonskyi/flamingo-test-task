package com.flamingo.qa.http.response;

import com.flamingo.qa.util.CustomLogger;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

import java.util.List;

/**
 * Wraps a REST Assured {@link Response} so callers work with {@link StatusCode} and typed
 * bodies instead of raw integers and JSON paths.
 *
 * <p>Clients return this for every call, positive or negative alike. The alternative - a
 * typed method for happy paths and a raw one for failures - forces a client method per
 * outcome and hides the status code the negative test is actually about.
 */
public final class ResponseWrapper {

    private static final CustomLogger log = CustomLogger.getLogger(ResponseWrapper.class);

    private final Response response;

    private ResponseWrapper(Response response) {
        this.response = response;
    }

    public static ResponseWrapper of(Response response) {
        ResponseWrapper wrapper = new ResponseWrapper(response);
        log.logResponse(wrapper.statusCodeValue(), wrapper.asString());
        return wrapper;
    }

    public ResponseVerifier verify() {
        return new ResponseVerifier(this);
    }

    public int statusCodeValue() {
        return response.getStatusCode();
    }

    public <T> T asPojo(Class<T> type) {
        return response.as(type);
    }

    /** Maps a nested part of the body, e.g. {@code data.characters}, onto a pojo. */
    public <T> T asPojoAt(String jsonPath, Class<T> type) {
        return response.jsonPath().getObject(jsonPath, type);
    }

    public JsonPath jsonPath() {
        return response.jsonPath();
    }

    /** For a list-of-objects body where one field is wanted, e.g. {@code [{"bookingid":1}]}. */
    public <T> List<T> asListOfField(String jsonPath, Class<T> type) {
        return response.jsonPath().getList(jsonPath, type);
    }

    public String asString() {
        return response.asString();
    }
}
