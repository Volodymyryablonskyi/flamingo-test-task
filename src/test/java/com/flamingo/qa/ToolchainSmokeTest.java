
package com.flamingo.qa;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import lombok.Builder;
import lombok.Value;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the <em>toolchain</em>, not the systems under test.
 *
 * <p>Each of these is a classic environment-level breakage point: Lombok annotation
 * processing silently producing no accessors, REST Assured failing on TLS, or Playwright
 * finding no installed browser. Catching those here keeps later failures attributable to
 * test logic rather than setup.
 *
 * <p>Superseded by the real suites in later phases; retained as a fast environment check.
 */
@Tag("smoke")
class ToolchainSmokeTest {

    @Value
    @Builder
    static class Sample {
        String name;
        int count;
    }

    @Test
    @DisplayName("Lombok annotation processing and AssertJ are wired correctly")
    void lombokAndAssertJAreWired() {
        Sample sample = Sample.builder().name("flamingo").count(3).build();

        assertThat(sample.getName()).isEqualTo("flamingo");
        assertThat(sample.getCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("REST Assured reaches the Restful Booker health endpoint")
    void restAssuredReachesRestfulBooker() {
        Response response = RestAssured.given()
                .baseUri("https://restful-booker.herokuapp.com")
                .when()
                .get("/ping")
                .andReturn();

        assertThat(response.statusCode()).isEqualTo(201);
    }

    @Test
    @DisplayName("Playwright launches the installed Chromium and renders a page")
    void playwrightLaunchesChromium() {
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium()
                     .launch(new BrowserType.LaunchOptions().setHeadless(true))) {

            Page page = browser.newPage();
            page.setContent("<h1 id='probe'>ready</h1>");

            assertThat(page.textContent("#probe")).isEqualTo("ready");
        }
    }
}