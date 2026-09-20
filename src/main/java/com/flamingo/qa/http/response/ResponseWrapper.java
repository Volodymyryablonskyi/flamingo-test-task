package com.flamingo.qa.http.response;

import com.flamingo.qa.util.CustomLogger;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

import java.util.List;

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

    public <T> T asPojoAt(String jsonPath, Class<T> type) {
        return response.jsonPath().getObject(jsonPath, type);
    }

    public JsonPath jsonPath() {
        return response.jsonPath();
    }

    public <T> List<T> asListAt(String jsonPath, Class<T> type) {
        return response.jsonPath().getList(jsonPath, type);
    }

    public String asString() {
        return response.asString();
    }
}
