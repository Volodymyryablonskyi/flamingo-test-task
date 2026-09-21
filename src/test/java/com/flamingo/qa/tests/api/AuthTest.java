package com.flamingo.qa.tests.api;

import com.flamingo.qa.base.BaseRestTest;
import com.flamingo.qa.clients.AuthApiClient;
import com.flamingo.qa.clients.TokenProvider;
import com.flamingo.qa.config.Config;
import com.flamingo.qa.pojo.auth.AuthResponse;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.flamingo.qa.http.response.StatusCode.STATUS_200_OK;
import static org.assertj.core.api.Assertions.assertThat;

@Feature("Authentication")
@DisplayName("POST /auth")
class AuthTest extends BaseRestTest {

    private final AuthApiClient authClient = new AuthApiClient();

    @Test
    @Tag("smoke")
    @Story("A valid login yields a token for subsequent requests")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("issues a token for valid credentials")
    void shouldIssueTokenForValidCredentials() {
        AuthResponse response = authClient.requestToken(Config.apiUsername(), Config.apiPassword())
                .verify().hasStatusCode(STATUS_200_OK)
                .and().asPojo(AuthResponse.class);

        assertThat(response.isSuccessful())
                .as("authentication outcome, which the status code does not reveal")
                .isTrue();
        assertThat(response.token()).isNotBlank();
        assertThat(response.reason()).isNull();
    }

    @Test
    @Story("A rejected login is distinguishable from a successful one")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("reports bad credentials in the body - with 200 OK, not 401")
    void shouldNotIssueTokenForInvalidCredentials() {
        AuthResponse response = authClient.requestToken(Config.apiUsername(), "not-the-password")
                .verify().hasStatusCode(STATUS_200_OK)
                .and().asPojo(AuthResponse.class);

        assertThat(response.isSuccessful()).isFalse();
        assertThat(response.token()).isNull();
        assertThat(response.reason()).isEqualTo("Bad credentials");
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
