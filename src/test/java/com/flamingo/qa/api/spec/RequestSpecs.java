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
 * The three request flavours the suite sends. Every request starts from one of them, so
 * base URI, timeouts, logging policy and the Allure filter are configured once.
 */
public final class RequestSpecs {

    /**
     * The literal string matters. REST Assured's {@code ContentType.JSON} expands to
     * {@code application/json, application/javascript, text/javascript, text/json}, and
     * Restful Booker answers any multi-value Accept header with HTTP 418 I'm a Teapot -
     * measured: {@code application/json} alone returns 200.
     */
    private static final String ACCEPT_JSON = "application/json";

    private RequestSpecs() {
    }

    public static RequestSpecification unauthenticated() {
        return base(Config.apiBaseUrl()).build();
    }

    /** Restful Booker expects the token as a {@code token} cookie, not a bearer header. */
    public static RequestSpecification authenticated(String token) {
        return base(Config.apiBaseUrl())
                .addCookie("token", token)
                .build();
    }

    public static RequestSpecification graphql() {
        return base(Config.graphqlUrl()).build();
    }

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
                .objectMapperConfig(ObjectMapperConfig.objectMapperConfig()
                        .jackson2ObjectMapperFactory((type, charset) -> Json.mapper()))
                // Quiet on green, full dump on red - readable console, diagnostic where it counts.
                .logConfig(LogConfig.logConfig()
                        .enableLoggingOfRequestAndResponseIfValidationFails(LogDetail.ALL))
                .httpClient(HttpClientConfig.httpClientConfig()
                        .setParam("http.connection.timeout", timeoutMs)
                        .setParam("http.socket.timeout", timeoutMs)
                        .setParam("http.connection-manager.timeout", (long) timeoutMs));
    }
}
