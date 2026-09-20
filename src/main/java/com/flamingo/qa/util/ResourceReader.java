package com.flamingo.qa.util;

import com.flamingo.qa.config.ConfigurationException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Reads fixtures from the classpath, so test data lives in {@code src/test/resources} as
 * real files - diffable, and editable without recompiling - instead of Java string literals.
 */
public final class ResourceReader {

    private ResourceReader() {
    }

    /** Parses a JSON array resource into a list, e.g. {@code testdata/guest-name-cases.json}. */
    public static <T> List<T> readList(String resourcePath, Class<T> elementType) {
        try (InputStream stream = open(resourcePath)) {
            return Json.mapper().readerForListOf(elementType).readValue(stream);
        } catch (IOException e) {
            throw new ConfigurationException("Could not parse classpath resource '" + resourcePath + "'.", e);
        }
    }

    private static InputStream open(String resourcePath) {
        InputStream stream = ResourceReader.class.getClassLoader().getResourceAsStream(resourcePath);
        if (stream == null) {
            throw new ConfigurationException(
                    "Classpath resource '" + resourcePath + "' was not found under src/test/resources.");
        }
        return stream;
    }
}
