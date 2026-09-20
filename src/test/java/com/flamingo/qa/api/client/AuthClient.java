package com.flamingo.qa.api.client;

import com.flamingo.qa.api.model.auth.AuthRequest;
import com.flamingo.qa.api.model.auth.AuthResponse;
import com.flamingo.qa.api.spec.RequestSpecs;
import com.flamingo.qa.api.support.TransientFailureRetry;
import io.qameta.allure.Step;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

/**
 * {@code POST /auth}.
 *
 * <p>Restful Booker answers <strong>HTTP 200 for a rejected login</strong>, with
 * {@code {"reason":"Bad credentials"}} in the body. That is unusual enough that the client
 * never infers success from the status code - callers ask {@link AuthResponse#isSuccessful()}.
 */
public class AuthClient {

    private static final String AUTH_PATH = "/auth";

    /** The typed body. Use this when the outcome is the subject of the assertion. */
    @Step("Request an auth token for user \"{username}\"")
    public AuthResponse requestToken(String username, String password) {
        return response(username, password).as(AuthResponse.class);
    }

    /** The raw response, for tests that assert on the status code or headers themselves. */
    @Step("Request an auth token for user \"{username}\" (raw response)")
    public Response response(String username, String password) {
        return TransientFailureRetry.send(() -> given()
                .spec(RequestSpecs.unauthenticated())
                .body(AuthRequest.builder().username(username).password(password).build())
                .when()
                .post(AUTH_PATH));
    }
}
