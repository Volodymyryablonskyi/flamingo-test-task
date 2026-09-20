package com.flamingo.qa.api.spec;

import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.ResponseSpecification;
import org.hamcrest.Matchers;

import java.time.Duration;

/**
 * Reusable response expectations - the "this call worked at all" half of a check, kept out
 * of the test bodies so the tests are left asserting business meaning.
 *
 * <p>Deliberately thin: these cover status, content type and a sanity ceiling on latency.
 * Everything about the payload is asserted in the test with AssertJ, where the failure
 * message is worth reading.
 */
public final class ResponseSpecs {

    /**
     * A generous ceiling, not a performance assertion: it turns a hung public demo service
     * into a clear failure instead of a stalled build.
     */
    private static final Duration RESPONSE_TIME_CEILING = Duration.ofSeconds(30);

    private ResponseSpecs() {
    }

    /** 200 with a JSON body - the shape of every successful call in this suite. */
    public static ResponseSpecification okJson() {
        return builder(200).expectContentType(ContentType.JSON).build();
    }

    /** Status only, for responses whose body is empty or not JSON (e.g. 201, 403, 404). */
    public static ResponseSpecification status(int expectedStatus) {
        return builder(expectedStatus).build();
    }

    private static ResponseSpecBuilder builder(int expectedStatus) {
        return new ResponseSpecBuilder()
                .expectStatusCode(expectedStatus)
                .expectResponseTime(Matchers.lessThan(RESPONSE_TIME_CEILING.toMillis()));
    }
}
