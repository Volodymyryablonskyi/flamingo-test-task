package com.flamingo.qa.util;

import com.flamingo.qa.config.ConfigurationException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class ResourceReader {

    private ResourceReader() {
    }

    public static String readString(String resourcePath) {
        try (InputStream stream = open(resourcePath)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ConfigurationException("Could not read classpath resource '" + resourcePath + "'.", e);
        }
    }

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
                    "Classpath resource '" + resourcePath + "' was not found on the classpath.");
        }
        return stream;
    }
}
