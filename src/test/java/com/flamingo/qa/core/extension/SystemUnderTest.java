package com.flamingo.qa.core.extension;

import com.flamingo.qa.core.config.Config;

import java.util.function.IntPredicate;
import java.util.function.Supplier;

/**
 * The external systems this suite exercises, each with the cheapest probe that proves it
 * is reachable and serving.
 *
 * <p>The URL is a {@link Supplier} rather than a constant so the probe follows whatever
 * {@link Config} resolves at runtime - point {@code graphql.url} at a different schema and
 * the health check follows it.
 */
public enum SystemUnderTest {

    /** Restful Booker answers its health endpoint with 201 Created, not 200. */
    RESTFUL_BOOKER("Restful Booker",
            () -> Config.apiBaseUrl() + "/ping",
            null,
            status -> status == 201 || status == 200),

    /** {@code __typename} is the smallest query every GraphQL schema can answer. */
    GRAPHQL("GraphQL API",
            Config::graphqlUrl,
            "{\"query\":\"{__typename}\"}",
            status -> status == 200),

    DEMOQA("DemoQA",
            Config::uiBaseUrl,
            null,
            status -> status == 200);

    private final String displayName;
    private final Supplier<String> url;
    private final String postBody;
    private final IntPredicate healthy;

    SystemUnderTest(String displayName, Supplier<String> url, String postBody, IntPredicate healthy) {
        this.displayName = displayName;
        this.url = url;
        this.postBody = postBody;
        this.healthy = healthy;
    }

    public String displayName() {
        return displayName;
    }

    public String url() {
        return url.get();
    }

    /** {@code null} means probe with GET. */
    String postBody() {
        return postBody;
    }

    boolean isHealthyStatus(int status) {
        return healthy.test(status);
    }
}
