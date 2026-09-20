package com.flamingo.qa.api.spec;

import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.ResponseSpecification;
import org.hamcrest.Matchers;

import java.time.Duration;

/**
 * Reusable "the call worked at all" expectations, kept out of the test bodies so tests are
 * left asserting business meaning.
 */
public final class ResponseSpecs {

    /**
     * Not a performance assertion. The socket timeout is per-read, so it cannot bound a
     * response that trickles bytes forever - this is the only guard on total duration, and
     * it is what caught a single call that took 372 seconds against a stalled dyno.
     */
    private static final Duration RESPONSE_TIME_CEILING = Duration.ofSeconds(30);

    private ResponseSpecs() {
    }

    public static ResponseSpecification okJson() {
        return builder(200).expectContentType(ContentType.JSON).build();
    }

    /** Status only, for responses whose body is empty or not JSON (201, 403, 404). */
    public static ResponseSpecification status(int expectedStatus) {
        return builder(expectedStatus).build();
    }

    private static ResponseSpecBuilder builder(int expectedStatus) {
        return new ResponseSpecBuilder()
                .expectStatusCode(expectedStatus)
                .expectResponseTime(Matchers.lessThan(RESPONSE_TIME_CEILING.toMillis()));
    }
}
