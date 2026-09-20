package com.flamingo.qa.api.client;

import com.flamingo.qa.api.model.auth.AuthRequest;
import com.flamingo.qa.api.model.auth.AuthResponse;
import com.flamingo.qa.api.spec.RequestSpecs;
import com.flamingo.qa.api.support.TransientFailureRetry;
import io.qameta.allure.Step;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

/**
 * {@code POST /auth}. Restful Booker answers a rejected login with HTTP 200 and
 * {@code {"reason":"Bad credentials"}}, so success is read from the body, never the status.
 */
public class AuthClient {

    private static final String AUTH_PATH = "/auth";

    @Step("Request an auth token for user \"{username}\"")
    public AuthResponse requestToken(String username, String password) {
        return response(username, password).as(AuthResponse.class);
    }

    @Step("Request an auth token for user \"{username}\" (raw response)")
    public Response response(String username, String password) {
        return TransientFailureRetry.send(() -> given()
                .spec(RequestSpecs.unauthenticated())
                .body(AuthRequest.builder().username(username).password(password).build())
                .when()
                .post(AUTH_PATH));
    }
}
