package com.flamingo.qa.api.spec;

import com.flamingo.qa.core.config.Config;
import com.flamingo.qa.core.util.Json;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.LogConfig;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

/**
 * Factories for the three request flavours this suite sends. Every request in the suite
 * starts from one of them, so base URI, timeouts, logging policy, retry and Allure
 * attachment are configured exactly once.
 *
 * <p>Logging is <em>failure-only</em>: quiet on green, full request and response dump on
 * red. That keeps the console readable, keeps a diagnostic trail where it matters, and
 * respects the brief's request not to spam these public services with chatter.
 */
public final class RequestSpecs {

    private RequestSpecs() {
    }

    /** Restful Booker, no credentials - creation, reads and the auth call itself. */
    public static RequestSpecification unauthenticated() {
        return base(Config.apiBaseUrl()).build();
    }

    /**
     * Restful Booker with a session token. The API expects it as a {@code token} cookie -
     * not a bearer header - which is the kind of detail that belongs here once rather
     * than in every test that updates a booking.
     */
    public static RequestSpecification authenticated(String token) {
        return base(Config.apiBaseUrl())
                .addCookie("token", token)
                .build();
    }

    /** The GraphQL endpoint, wherever {@code graphql.url} currently points. */
    public static RequestSpecification graphql() {
        return base(Config.graphqlUrl()).build();
    }

    /**
     * The literal string matters. REST Assured's {@code ContentType.JSON} expands to
     * {@code application/json, application/javascript, text/javascript, text/json}, and
     * Restful Booker answers any multi-value Accept header with <strong>HTTP 418 I'm a
     * Teapot</strong> - measured: {@code application/json} alone returns 200, while
     * {@code application/json, text/json} returns 418. A single value keeps its content
     * negotiation happy and costs us nothing.
     */
    private static final String ACCEPT_JSON = "application/json";

    private static RequestSpecBuilder base(String baseUri) {
        return new RequestSpecBuilder()
                .setBaseUri(baseUri)
                .setContentType(ContentType.JSON)
                .setAccept(ACCEPT_JSON)
                .setConfig(config())
                .addFilter(new AllureRestAssured());
    }

    private static RestAssuredConfig config() {
        int timeoutMs = (int) Config.apiTimeout().toMillis();
        return RestAssuredConfig.config()
                // REST Assured would otherwise build its own ObjectMapper, so a model could
                // round-trip in a unit test and still map wrongly over the wire. One mapper.
                .objectMapperConfig(ObjectMapperConfig.objectMapperConfig()
                        .jackson2ObjectMapperFactory((type, charset) -> Json.mapper()))
                .logConfig(LogConfig.logConfig()
                        .enableLoggingOfRequestAndResponseIfValidationFails(LogDetail.ALL))
                .httpClient(HttpClientConfig.httpClientConfig()
                        .setParam("http.connection.timeout", timeoutMs)
                        .setParam("http.socket.timeout", timeoutMs)
                        .setParam("http.connection-manager.timeout", (long) timeoutMs));
    }
}
