package com.flamingo.qa.extensions;

import com.flamingo.qa.config.Config;

import java.util.function.IntPredicate;
import java.util.function.Supplier;

public enum SystemUnderTest {

    RESTFUL_BOOKER("Restful Booker",
            () -> Config.apiBaseUrl() + "/ping",
            null,
            status -> status == 201 || status == 200),

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

    String postBody() {
        return postBody;
    }

    boolean isHealthyStatus(int status) {
        return healthy.test(status);
    }
}
