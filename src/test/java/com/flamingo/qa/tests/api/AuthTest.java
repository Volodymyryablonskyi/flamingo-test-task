package com.flamingo.qa.tests.api;

import com.flamingo.qa.api.client.AuthClient;
import com.flamingo.qa.api.client.TokenProvider;
import com.flamingo.qa.api.model.auth.AuthResponse;
import com.flamingo.qa.core.config.Config;
import com.flamingo.qa.tests.BaseRestTest;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Feature("Authentication")
@DisplayName("POST /auth")
class AuthTest extends BaseRestTest {

    private final AuthClient authClient = new AuthClient();

    @Test
    @Tag("smoke")
    @Story("A valid login yields a token for subsequent requests")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("issues a token for valid credentials")
    void shouldIssueTokenForValidCredentials() {
        AuthResponse response = authClient.requestToken(Config.apiUsername(), Config.apiPassword());

        assertThat(response.isSuccessful())
                .as("authentication outcome, which the status code does not reveal")
                .isTrue();
        assertThat(response.getToken()).isNotBlank();
        assertThat(response.getReason()).isNull();
    }

    @Test
    @Story("A rejected login is distinguishable from a successful one")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("reports bad credentials in the body - with HTTP 200, not 401")
    void shouldNotIssueTokenForInvalidCredentials() {
        Response response = authClient.response(Config.apiUsername(), "definitely-not-the-password");

        // Asserting the contract the API has, not the one it ought to have: expecting 401
        // here would fail against a working service and tell us nothing.
        assertThat(response.statusCode()).isEqualTo(200);

        AuthResponse body = response.as(AuthResponse.class);
        assertThat(body.isSuccessful()).isFalse();
        assertThat(body.getToken()).isNull();
        assertThat(body.getReason()).isEqualTo("Bad credentials");
    }

    @Test
    @Story("A valid login yields a token for subsequent requests")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("caches one token per JVM instead of re-authenticating per test")
    void shouldReuseTheSameTokenAcrossCalls() {
        assertThat(TokenProvider.token())
                .isNotBlank()
                .isSameAs(TokenProvider.token());
    }
}
