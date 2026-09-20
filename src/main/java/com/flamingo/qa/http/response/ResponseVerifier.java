package com.flamingo.qa.http.response;

import com.flamingo.qa.util.CustomLogger;
import org.assertj.core.api.Assertions;

/**
 * Fluent, AssertJ-backed checks on a response. Chainable, and {@link #and()} hands the
 * wrapper back so a test reads:
 *
 * <pre>{@code
 * Booking booking = client.getById(id)
 *         .verify().hasStatusCode(STATUS_200_OK)
 *         .and().asPojo(Booking.class);
 * }</pre>
 */
public final class ResponseVerifier {

    private static final CustomLogger log = CustomLogger.getLogger(ResponseVerifier.class);

    private final ResponseWrapper wrapper;

    ResponseVerifier(ResponseWrapper wrapper) {
        this.wrapper = wrapper;
    }

    public ResponseWrapper and() {
        return wrapper;
    }

    public ResponseVerifier hasStatusCode(StatusCode expected) {
        int actual = wrapper.statusCodeValue();
        log.debug("Verify status code is {}", expected);
        // Compared as ints so an unmapped actual code still produces a readable message
        // instead of an IllegalArgumentException from the enum lookup.
        Assertions.assertThat(actual)
                .as("expected %s but got %s - body: %s",
                        expected, StatusCode.describe(actual), wrapper.asString())
                .isEqualTo(expected.getCode());
        return this;
    }

    public ResponseVerifier hasBodyEqualTo(String expected) {
        Assertions.assertThat(wrapper.asString().trim()).isEqualTo(expected);
        return this;
    }

    /**
     * Asserts a JSON path is present and non-null. In GraphQL the body of interest is
     * nested under {@code data}, and the presence of {@code errors} is itself the contract.
     */
    public ResponseVerifier hasJsonField(String jsonPath) {
        Assertions.assertThat((Object) wrapper.jsonPath().get(jsonPath))
                .as("JSON path '%s' in %s", jsonPath, wrapper.asString())
                .isNotNull();
        return this;
    }

    public ResponseVerifier hasNoJsonField(String jsonPath) {
        Assertions.assertThat((Object) wrapper.jsonPath().get(jsonPath))
                .as("JSON path '%s' should be absent in %s", jsonPath, wrapper.asString())
                .isNull();
        return this;
    }

    /** Field-by-field, so a mismatch names the field rather than dumping two objects. */
    public <T> ResponseVerifier hasBodyEqualTo(Class<T> type, T expected) {
        Assertions.assertThat(wrapper.asPojo(type))
                .usingRecursiveComparison()
                .isEqualTo(expected);
        return this;
    }
}
