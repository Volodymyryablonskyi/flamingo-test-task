package com.flamingo.qa.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;
import java.util.function.UnaryOperator;

/**
 * Resolves a key against a system property, then {@code API_BASE_URL}-style environment
 * variable, then {@code config.properties}; a key that resolves nowhere throws instead of
 * returning null.
 *
 * <p>The lookups are injected so the precedence chain is unit-testable - a JVM cannot set
 * its own environment variables.
 */
public final class ConfigLoader {

    private static final String DEFAULTS_RESOURCE = "config.properties";

    private static final ConfigLoader INSTANCE =
            new ConfigLoader(loadDefaults(), System::getenv, System::getProperty);

    private final Properties defaults;
    private final UnaryOperator<String> environment;
    private final UnaryOperator<String> systemProperties;

    /** Public so the precedence chain can be exercised with injected sources. */
    public ConfigLoader(Properties defaults,
                        UnaryOperator<String> environment,
                        UnaryOperator<String> systemProperties) {
        this.defaults = defaults;
        this.environment = environment;
        this.systemProperties = systemProperties;
    }

    public static ConfigLoader instance() {
        return INSTANCE;
    }

    public String get(String key) {
        String value = resolve(key);
        if (value == null) {
            throw new ConfigurationException(
                    "No value for configuration key '" + key + "'. Set it with -D" + key
                            + ", with the " + environmentVariableName(key)
                            + " environment variable, or in " + DEFAULTS_RESOURCE + ".");
        }
        return value;
    }

    public int getInt(String key) {
        String value = get(key);
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new ConfigurationException(
                    "Configuration key '" + key + "' must be an integer but was '" + value + "'.", e);
        }
    }

    /** Strict: {@code Boolean.parseBoolean} reads a typo like {@code ture} as false. */
    public boolean getBoolean(String key) {
        String value = get(key).trim();
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        throw new ConfigurationException(
                "Configuration key '" + key + "' must be 'true' or 'false' but was '" + value + "'.");
    }

    private String resolve(String key) {
        String fromSystemProperty = systemProperties.apply(key);
        if (isPresent(fromSystemProperty)) {
            return fromSystemProperty.trim();
        }
        String fromEnvironment = environment.apply(environmentVariableName(key));
        if (isPresent(fromEnvironment)) {
            return fromEnvironment.trim();
        }
        String fromDefaults = defaults.getProperty(key);
        return isPresent(fromDefaults) ? fromDefaults.trim() : null;
    }

    public static String environmentVariableName(String key) {
        return key.toUpperCase(Locale.ROOT).replace('.', '_');
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    private static Properties loadDefaults() {
        Properties properties = new Properties();
        try (InputStream stream = ConfigLoader.class.getClassLoader()
                .getResourceAsStream(DEFAULTS_RESOURCE)) {
            if (stream == null) {
                throw new ConfigurationException(
                        DEFAULTS_RESOURCE + " was not found on the test classpath.");
            }
            properties.load(stream);
        } catch (IOException e) {
            throw new ConfigurationException("Could not read " + DEFAULTS_RESOURCE + ".", e);
        }
        return properties;
    }
}
