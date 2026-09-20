package com.flamingo.qa.core.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * The one ObjectMapper the suite uses. REST Assured builds its own by default, which means
 * a model can round-trip in a unit test and still map wrongly over the wire; handing it
 * this instance removes that gap.
 */
public final class Json {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            // Third-party APIs are free to add fields; drift we care about is asserted explicitly.
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private Json() {
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }
}
