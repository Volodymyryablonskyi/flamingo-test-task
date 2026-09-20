package com.flamingo.qa.clients;

import com.flamingo.qa.endpoints.Endpoints;
import com.flamingo.qa.http.request.HttpMethod;
import com.flamingo.qa.http.request.RequestBuilder;
import com.flamingo.qa.http.response.ResponseWrapper;
import com.flamingo.qa.http.response.StatusCode;
import com.flamingo.qa.util.CustomLogger;
import io.restassured.specification.RequestSpecification;

import java.util.Map;
import java.util.function.Supplier;

public abstract class BaseApiClient<T extends Endpoints> {

    private static final CustomLogger log = CustomLogger.getLogger(BaseApiClient.class);

    protected final T endpoints;

    private final Supplier<RequestSpecification> spec;
    private final boolean reauthenticatesOn403;

    protected BaseApiClient(Supplier<RequestSpecification> spec, T endpoints) {
        this(spec, endpoints, false);
    }

    protected BaseApiClient(Supplier<RequestSpecification> spec, T endpoints,
                            boolean reauthenticatesOn403) {
        this.spec = spec;
        this.endpoints = endpoints;
        this.reauthenticatesOn403 = reauthenticatesOn403;
    }

    protected ResponseWrapper request(HttpMethod method, String path) {
        return request(method, path, null, null);
    }

    protected ResponseWrapper request(HttpMethod method, String path, Object body) {
        return request(method, path, body, null);
    }

    protected ResponseWrapper request(HttpMethod method, String path, Object body,
                                      Map<String, ?> queryParams) {
        ResponseWrapper response = send(method, path, body, queryParams);

        if (reauthenticatesOn403 && response.statusCodeValue() == StatusCode.STATUS_403_FORBIDDEN.getCode()) {
            log.warn("{} {} was rejected with 403 - refreshing the auth token and retrying once",
                    method, path);
            TokenProvider.invalidate();
            response = send(method, path, body, queryParams);
        }
        return response;
    }

    private ResponseWrapper send(HttpMethod method, String path, Object body,
                                 Map<String, ?> queryParams) {
        return new RequestBuilder(spec.get())
                .withMethod(method)
                .withPath(path)
                .withBody(body)
                .withQueryParams(queryParams)
                .send();
    }
}
