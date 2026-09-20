package com.flamingo.qa.config;

import com.flamingo.qa.util.Json;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.LogConfig;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RedirectConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

/**
 * Builds the request specifications. Every request in the suite starts from one of these,
 * so base URI, timeouts, logging policy and the Allure filter are configured once.
 */
public final class RestAssuredConfigurator {

    /**
     * The literal string matters. REST Assured's {@code ContentType.JSON} expands to
     * {@code application/json, application/javascript, text/javascript, text/json}, and
     * Restful Booker answers any multi-value Accept header with 418 I'm a Teapot -
     * measured: {@code application/json} alone returns 200.
     */
    private static final String ACCEPT_JSON = "application/json";

    private RestAssuredConfigurator() {
    }

    public static RequestSpecification restSpec() {
        return specFor(Config.apiBaseUrl()).build();
    }

    /** Restful Booker expects the token as a {@code token} cookie, not a bearer header. */
    public static RequestSpecification authenticatedRestSpec(String token) {
        return specFor(Config.apiBaseUrl()).addCookie("token", token).build();
    }

    public static RequestSpecification graphqlSpec() {
        return specFor(Config.graphqlUrl()).build();
    }

    private static RequestSpecBuilder specFor(String baseUri) {
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
                // REST Assured would otherwise build its own ObjectMapper, so a pojo could
                // round-trip in a unit test and still map wrongly over the wire.
                .objectMapperConfig(ObjectMapperConfig.objectMapperConfig()
                        .jackson2ObjectMapperFactory((type, charset) -> Json.mapper()))
                // Quiet on green, full dump on red.
                .logConfig(LogConfig.logConfig()
                        .enableLoggingOfRequestAndResponseIfValidationFails(LogDetail.ALL))
                .redirect(RedirectConfig.redirectConfig().followRedirects(true).maxRedirects(10))
                .httpClient(HttpClientConfig.httpClientConfig()
                        .setParam("http.connection.timeout", timeoutMs)
                        .setParam("http.socket.timeout", timeoutMs)
                        .setParam("http.connection-manager.timeout", (long) timeoutMs));
    }
}
