package com.flamingo.qa.endpoints;

/**
 * A GraphQL API has exactly one URL by design, and {@code graphql.url} already carries it
 * in full so the endpoint can be repointed at another schema from configuration. The path
 * relative to that base is therefore empty.
 */
public class GraphQlEndpoints implements Endpoints {

    private static final String ROOT = "";

    public String getQueryUri() {
        return ROOT;
    }
}
