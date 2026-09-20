package com.flamingo.qa.http.request;

import com.flamingo.qa.http.response.ResponseWrapper;
import com.flamingo.qa.http.retry.TransientFailureRetry;
import com.flamingo.qa.util.CustomLogger;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Assembles and sends one request. Keeping the REST Assured fluent chain here means a
 * client method is a single readable line and the verb switch exists exactly once.
 *
 * <p>The send goes through {@link TransientFailureRetry}, which sits below every assertion,
 * so a retried request never re-runs a check.
 */
public class RequestBuilder {

    private static final CustomLogger log = CustomLogger.getLogger(RequestBuilder.class);

    private final RequestSpecification spec;
    private final Map<String, Object> queryParams = new LinkedHashMap<>();
    private final Map<String, String> headers = new LinkedHashMap<>();

    private HttpMethod method;
    private String path;
    private Object body;

    public RequestBuilder(RequestSpecification spec) {
        this.spec = spec;
    }

    public RequestBuilder withMethod(HttpMethod method) {
        this.method = method;
        return this;
    }

    public RequestBuilder withPath(String path) {
        this.path = path;
        return this;
    }

    public RequestBuilder withBody(Object body) {
        this.body = body;
        return this;
    }

    public RequestBuilder withQueryParams(Map<String, ?> params) {
        if (params != null) {
            queryParams.putAll(params);
        }
        return this;
    }

    public RequestBuilder withHeaders(Map<String, String> extraHeaders) {
        if (extraHeaders != null) {
            headers.putAll(extraHeaders);
        }
        return this;
    }

    public ResponseWrapper send() {
        if (method == null || path == null) {
            throw new IllegalStateException("A request needs both a method and a path.");
        }
        log.logRequest(method, path, queryParams, body);

        return ResponseWrapper.of(TransientFailureRetry.send(() -> {
            RequestSpecification request = RestAssured.given().spec(spec);
            if (!queryParams.isEmpty()) {
                request.queryParams(queryParams);
            }
            if (!headers.isEmpty()) {
                request.headers(headers);
            }
            if (body != null) {
                request.body(body);
            }
            return switch (method) {
                case GET -> request.get(path);
                case POST -> request.post(path);
                case PUT -> request.put(path);
                case PATCH -> request.patch(path);
                case DELETE -> request.delete(path);
            };
        }));
    }
}
