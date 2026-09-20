package com.flamingo.qa.config;

import java.time.Duration;

/**
 * Typed view of the suite's configuration. Nothing outside this class knows a property key,
 * so a typo is a compile error rather than a null.
 */
public final class Config {

    private Config() {
    }

    public static String apiBaseUrl() {
        return loader().get("api.base.url");
    }

    public static String apiUsername() {
        return loader().get("api.auth.username");
    }

    public static String apiPassword() {
        return loader().get("api.auth.password");
    }

    public static Duration apiTimeout() {
        return Duration.ofMillis(loader().getInt("api.timeout.ms"));
    }

    /** Total attempts including the first; 1 disables retrying. */
    public static int apiRetryMaxAttempts() {
        return loader().getInt("api.retry.max.attempts");
    }

    public static Duration apiRetryBackoff() {
        return Duration.ofMillis(loader().getInt("api.retry.backoff.ms"));
    }

    public static String graphqlUrl() {
        return loader().get("graphql.url");
    }

    public static String uiBaseUrl() {
        return loader().get("ui.base.url");
    }

    public static boolean headless() {
        return loader().getBoolean("ui.headless");
    }

    public static Duration uiTimeout() {
        return Duration.ofMillis(loader().getInt("ui.timeout.ms"));
    }

    public static Duration uiSlowMo() {
        return Duration.ofMillis(loader().getInt("ui.slowmo.ms"));
    }

    public static int viewportWidth() {
        return loader().getInt("ui.viewport.width");
    }

    public static int viewportHeight() {
        return loader().getInt("ui.viewport.height");
    }

    public static boolean traceEnabled() {
        return loader().getBoolean("ui.trace.enabled");
    }

    public static boolean healthCheckEnabled() {
        return loader().getBoolean("health.check.enabled");
    }

    public static Duration healthCheckTimeout() {
        return Duration.ofMillis(loader().getInt("health.timeout.ms"));
    }

    private static ConfigLoader loader() {
        return ConfigLoader.instance();
    }
}
