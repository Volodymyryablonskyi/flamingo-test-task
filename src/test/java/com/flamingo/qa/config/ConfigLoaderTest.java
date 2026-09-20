package com.flamingo.qa.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The precedence chain is machinery every other test depends on, so it is verified directly
 * rather than inferred from a test that happened to pass. The lookups are injected because
 * a JVM cannot set its own environment variables.
 */
@Tag("unit")
@DisplayName("ConfigLoader")
class ConfigLoaderTest {

    private static final String KEY = "api.base.url";

    private static ConfigLoader loaderWith(Map<String, String> environment,
                                           Map<String, String> systemProperties,
                                           Map<String, String> defaults) {
        Properties properties = new Properties();
        defaults.forEach(properties::setProperty);
        return new ConfigLoader(properties, lookup(environment), lookup(systemProperties));
    }

    private static UnaryOperator<String> lookup(Map<String, String> source) {
        return source::get;
    }

    @Nested
    @DisplayName("resolves in precedence order")
    class Precedence {

        @Test
        @DisplayName("a system property beats an environment variable and the defaults file")
        void systemPropertyWins() {
            ConfigLoader loader = loaderWith(
                    Map.of("API_BASE_URL", "https://from-environment"),
                    Map.of(KEY, "https://from-system-property"),
                    Map.of(KEY, "https://from-defaults"));

            assertThat(loader.get(KEY)).isEqualTo("https://from-system-property");
        }

        @Test
        @DisplayName("an environment variable beats the defaults file")
        void environmentVariableWins() {
            ConfigLoader loader = loaderWith(
                    Map.of("API_BASE_URL", "https://from-environment"),
                    Map.of(),
                    Map.of(KEY, "https://from-defaults"));

            assertThat(loader.get(KEY)).isEqualTo("https://from-environment");
        }

        @Test
        @DisplayName("the defaults file is used when nothing is overridden")
        void defaultsAreTheFallback() {
            ConfigLoader loader = loaderWith(Map.of(), Map.of(), Map.of(KEY, "https://from-defaults"));

            assertThat(loader.get(KEY)).isEqualTo("https://from-defaults");
        }

        @Test
        @DisplayName("a blank override is ignored rather than treated as a value")
        void blankOverridesFallThrough() {
            ConfigLoader loader = loaderWith(
                    Map.of("API_BASE_URL", "   "),
                    Map.of(KEY, ""),
                    Map.of(KEY, "https://from-defaults"));

            assertThat(loader.get(KEY)).isEqualTo("https://from-defaults");
        }
    }

    @Nested
    @DisplayName("fails loudly")
    class Failures {

        @Test
        @DisplayName("naming the key and both override mechanisms when it resolves nowhere")
        void unknownKeyNamesItself() {
            ConfigLoader loader = loaderWith(Map.of(), Map.of(), Map.of());

            assertThatThrownBy(() -> loader.get("ui.base.url"))
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("ui.base.url")
                    .hasMessageContaining("UI_BASE_URL");
        }

        @Test
        @DisplayName("when a numeric key holds something that is not a number")
        void nonNumericIntIsRejected() {
            ConfigLoader loader = loaderWith(Map.of(), Map.of(), Map.of("api.timeout.ms", "soon"));

            assertThatThrownBy(() -> loader.getInt("api.timeout.ms"))
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("must be an integer");
        }

        @Test
        @DisplayName("on a misspelled boolean, which Boolean.parseBoolean would read as false")
        void misspelledBooleanIsRejected() {
            ConfigLoader loader = loaderWith(Map.of(), Map.of(), Map.of("ui.headless", "ture"));

            assertThatThrownBy(() -> loader.getBoolean("ui.headless"))
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("must be 'true' or 'false'");
        }
    }

    @Nested
    @DisplayName("committed defaults")
    class CommittedDefaults {

        @Test
        @DisplayName("cover every key the suite reads, so a cold clone needs no setup")
        void everyTypedAccessorResolves() {
            assertThat(Config.apiBaseUrl()).startsWith("https://");
            assertThat(Config.apiUsername()).isNotBlank();
            assertThat(Config.apiPassword()).isNotBlank();
            assertThat(Config.apiTimeout().toMillis()).isPositive();
            assertThat(Config.apiRetryMaxAttempts()).isGreaterThanOrEqualTo(1);
            assertThat(Config.apiRetryBackoff().toMillis()).isPositive();
            assertThat(Config.graphqlUrl()).startsWith("https://");
            assertThat(Config.uiBaseUrl()).startsWith("https://");
            assertThat(Config.headless()).isTrue();
            assertThat(Config.uiTimeout().toMillis()).isPositive();
            assertThat(Config.uiSlowMo().toMillis()).isNotNegative();
            assertThat(Config.viewportWidth()).isPositive();
            assertThat(Config.viewportHeight()).isPositive();
            assertThat(Config.traceEnabled()).isTrue();
            assertThat(Config.healthCheckEnabled()).isTrue();
            assertThat(Config.healthCheckTimeout().toMillis()).isPositive();
        }

        @Test
        @DisplayName("map to environment variables by upper-casing and replacing dots")
        void environmentVariableNamesAreDerivedMechanically() {
            assertThat(ConfigLoader.environmentVariableName("api.base.url")).isEqualTo("API_BASE_URL");
            assertThat(ConfigLoader.environmentVariableName("ui.headless")).isEqualTo("UI_HEADLESS");
        }
    }
}
