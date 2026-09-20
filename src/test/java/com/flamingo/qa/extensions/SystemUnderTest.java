package com.flamingo.qa.extensions;

import com.flamingo.qa.config.Config;

import java.util.function.IntPredicate;
import java.util.function.Supplier;

/** The external systems the suite exercises, each with the cheapest probe that proves it is serving. */
public enum SystemUnderTest {

    /** Restful Booker answers its health endpoint with 201, not 200. */
    RESTFUL_BOOKER("Restful Booker",
            () -> Config.apiBaseUrl() + "/ping",
            null,
            status -> status == 201 || status == 200),

    /** {@code __typename} is the smallest query any GraphQL schema can answer. */
    GRAPHQL("GraphQL API",
            Config::graphqlUrl,
            "{\"query\":\"{__typename}\"}",
            status -> status == 200),

    DEMOQA("DemoQA",
            Config::uiBaseUrl,
            null,
            status -> status == 200);

    private final String displayName;
    /** A supplier, not a constant, so the probe follows whatever Config resolves at runtime. */
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
