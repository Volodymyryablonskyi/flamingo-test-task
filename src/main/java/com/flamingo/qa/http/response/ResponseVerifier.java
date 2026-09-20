package com.flamingo.qa.http.response;

import com.flamingo.qa.util.CustomLogger;
import org.assertj.core.api.Assertions;

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

    public <T> ResponseVerifier hasBodyEqualTo(Class<T> type, T expected) {
        Assertions.assertThat(wrapper.asPojo(type))
                .usingRecursiveComparison()
                .isEqualTo(expected);
        return this;
    }
}
