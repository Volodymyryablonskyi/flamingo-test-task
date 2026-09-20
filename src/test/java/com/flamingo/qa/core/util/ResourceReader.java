package com.flamingo.qa.core.util;

import com.flamingo.qa.core.config.ConfigurationException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Reads files from the test classpath.
 *
 * <p>Exists so GraphQL documents and JSON fixtures can live in {@code src/test/resources}
 * as real files - syntax-highlighted, diffable, and reusable - instead of being pasted
 * into Java string literals.
 */
public final class ResourceReader {

    private ResourceReader() {
    }

    /** Reads a UTF-8 classpath resource in full, e.g. {@code graphql/characters-page.graphql}. */
    public static String readString(String resourcePath) {
        try (InputStream stream = open(resourcePath)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ConfigurationException("Could not read classpath resource '" + resourcePath + "'.", e);
        }
    }

    /**
     * Resolves a classpath resource to a filesystem path - needed by Playwright's file
     * upload, which takes a {@link Path} rather than a stream.
     */
    public static Path resolvePath(String resourcePath) {
        try {
            return Paths.get(url(resourcePath).toURI());
        } catch (URISyntaxException e) {
            throw new ConfigurationException("Classpath resource '" + resourcePath + "' is not a file.", e);
        }
    }

    private static InputStream open(String resourcePath) {
        InputStream stream = classLoader().getResourceAsStream(resourcePath);
        if (stream == null) {
            throw new ConfigurationException(missingMessage(resourcePath));
        }
        return stream;
    }

    private static java.net.URL url(String resourcePath) {
        java.net.URL url = classLoader().getResource(resourcePath);
        if (url == null) {
            throw new ConfigurationException(missingMessage(resourcePath));
        }
        return url;
    }

    private static String missingMessage(String resourcePath) {
        return "Classpath resource '" + resourcePath + "' was not found under src/test/resources.";
    }

    private static ClassLoader classLoader() {
        return ResourceReader.class.getClassLoader();
    }
}
