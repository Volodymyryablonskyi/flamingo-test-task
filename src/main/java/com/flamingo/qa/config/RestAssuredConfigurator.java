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

public final class RestAssuredConfigurator {

    private static final String SINGLE_VALUE_ACCEPT_JSON = "application/json";

    private RestAssuredConfigurator() {
    }

    public static RequestSpecification restSpec() {
        return specFor(Config.apiBaseUrl()).build();
    }

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
                .setAccept(SINGLE_VALUE_ACCEPT_JSON)
                .setConfig(config())
                .addFilter(new AllureRestAssured());
    }

    private static RestAssuredConfig config() {
        int timeoutMs = (int) Config.apiTimeout().toMillis();
        return RestAssuredConfig.config()
                .objectMapperConfig(ObjectMapperConfig.objectMapperConfig()
                        .jackson2ObjectMapperFactory((type, charset) -> Json.mapper()))
                .logConfig(LogConfig.logConfig()
                        .enableLoggingOfRequestAndResponseIfValidationFails(LogDetail.ALL))
                .redirect(RedirectConfig.redirectConfig().followRedirects(true).maxRedirects(10))
                .httpClient(HttpClientConfig.httpClientConfig()
                        .setParam("http.connection.timeout", timeoutMs)
                        .setParam("http.socket.timeout", timeoutMs)
                        .setParam("http.connection-manager.timeout", (long) timeoutMs));
    }
}
