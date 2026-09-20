package com.flamingo.qa.http.response;

import com.flamingo.qa.util.CustomLogger;
import io.restassured.response.Response;

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

    public String asString() {
        return response.asString();
    }
}
