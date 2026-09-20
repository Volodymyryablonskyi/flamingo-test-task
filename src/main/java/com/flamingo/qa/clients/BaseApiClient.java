package com.flamingo.qa.clients;

import com.flamingo.qa.endpoints.Endpoints;
import com.flamingo.qa.http.request.HttpMethod;
import com.flamingo.qa.http.request.RequestBuilder;
import com.flamingo.qa.http.response.ResponseWrapper;
import io.restassured.specification.RequestSpecification;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Shared plumbing for the API clients: it owns the spec and turns a method, path and
 * optional body into a {@link ResponseWrapper}.
 *
 * <p>The spec is a {@link Supplier} rather than a value so an authenticated client resolves
 * its token on each call instead of at construction, which matters because clients are
 * built once per test class while the token is fetched lazily.
 *
 * @param <T> the endpoint group this client calls
 */
public abstract class BaseApiClient<T extends Endpoints> {

    protected final T endpoints;

    private final Supplier<RequestSpecification> spec;

    protected BaseApiClient(Supplier<RequestSpecification> spec, T endpoints) {
        this.spec = spec;
        this.endpoints = endpoints;
    }

    protected ResponseWrapper request(HttpMethod method, String path) {
        return request(method, path, null, null);
    }

    protected ResponseWrapper request(HttpMethod method, String path, Object body) {
        return request(method, path, body, null);
    }

    protected ResponseWrapper request(HttpMethod method, String path, Object body,
                                      Map<String, ?> queryParams) {
        return new RequestBuilder(spec.get())
                .withMethod(method)
                .withPath(path)
                .withBody(body)
                .withQueryParams(queryParams)
                .send();
    }
}
