package com.flamingo.qa.core.config;

import java.time.Duration;

/**
 * The single typed view of the suite's configuration.
 *
 * <p>Nothing outside this class knows a property key. Callers say
 * {@code Config.apiBaseUrl()}, so a typo is a compile error instead of a {@code null} that
 * only shows up as a malformed URL three layers down.
 *
 * <p>Values are read on each call rather than cached in static finals: that keeps the
 * class free of initialisation order surprises and lets a test override a system property
 * and observe the effect.
 */
public final class Config {

    private Config() {
    }

    // ===== REST =====

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

    /** Total attempts, including the first one; 1 disables retrying. */
    public static int apiRetryMaxAttempts() {
        return loader().getInt("api.retry.max.attempts");
    }

    public static Duration apiRetryBackoff() {
        return Duration.ofMillis(loader().getInt("api.retry.backoff.ms"));
    }

    // ===== GraphQL =====

    public static String graphqlUrl() {
        return loader().get("graphql.url");
    }

    // ===== UI =====

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

    // ===== Health checks =====

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
