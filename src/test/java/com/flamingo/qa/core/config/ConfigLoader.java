package com.flamingo.qa.core.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.function.UnaryOperator;

/**
 * Resolves a configuration key against three sources, first hit wins:
 *
 * <ol>
 *   <li>a system property - {@code -Dapi.base.url=...}, for ad-hoc local overrides;</li>
 *   <li>an environment variable - {@code API_BASE_URL}, how CI overrides things;</li>
 *   <li>{@code config.properties} on the test classpath - the committed defaults.</li>
 * </ol>
 *
 * <p>A key that is missing from all three is a configuration bug, not a {@code null} to be
 * propagated: {@link #get(String)} throws immediately and names the key, so the failure
 * surfaces where it can be fixed rather than as an NPE inside a request builder.
 *
 * <p>The environment-variable name is derived mechanically - upper-case, dots to
 * underscores - so there is no mapping table to keep in sync.
 *
 * <p>The lookup functions are injected rather than called statically so the precedence
 * chain itself is unit-testable; production code uses {@link #instance()}.
 */
public final class ConfigLoader {

    private static final String DEFAULTS_RESOURCE = "config.properties";

    private static final ConfigLoader INSTANCE =
            new ConfigLoader(loadDefaults(), System::getenv, System::getProperty);

    private final Properties defaults;
    private final UnaryOperator<String> environment;
    private final UnaryOperator<String> systemProperties;

    ConfigLoader(Properties defaults,
                 UnaryOperator<String> environment,
                 UnaryOperator<String> systemProperties) {
        this.defaults = defaults;
        this.environment = environment;
        this.systemProperties = systemProperties;
    }

    public static ConfigLoader instance() {
        return INSTANCE;
    }

    /**
     * @throws ConfigurationException if the key resolves nowhere
     */
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

    /**
     * Strict on purpose: a typo such as {@code ui.headless=ture} silently means
     * {@code false} to {@link Boolean#parseBoolean}, which would quietly open a browser
     * window on a CI runner.
     */
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

    static String environmentVariableName(String key) {
        return key.toUpperCase(java.util.Locale.ROOT).replace('.', '_');
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
