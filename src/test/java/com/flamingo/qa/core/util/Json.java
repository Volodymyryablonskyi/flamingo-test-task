package com.flamingo.qa.core.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * The one Jackson {@link ObjectMapper} the suite uses.
 *
 * <p>REST Assured creates its own mapper by default, which means a model that round-trips
 * correctly in a unit test can still deserialise wrongly in a request. Handing this
 * instance to REST Assured (see {@code RequestSpecs}) removes that gap - there is exactly
 * one JSON configuration in the project.
 */
public final class Json {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            // Dates as "2026-10-01", not as an epoch array - what the APIs actually send.
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            // A field we do not model is not a test failure: these are third-party APIs and
            // they are free to add fields. Contract drift we care about is asserted explicitly.
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private Json() {
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }
}
