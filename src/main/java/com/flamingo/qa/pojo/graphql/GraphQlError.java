package com.flamingo.qa.pojo.graphql;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Map;

/**
 * One entry of a GraphQL {@code errors} array.
 *
 * <p>{@code extensions} stays an untyped map because it is the deliberately open part of
 * the spec - servers and CDNs in front of them put different things there, and this schema
 * is swappable by configuration.
 */
@Value
@Builder
@Jacksonized
public class GraphQlError {

    String message;
    List<Location> locations;
    Map<String, Object> extensions;

    /** @return the {@code extensions.code} of this error, or null when it carries none */
    public String extensionCode() {
        return extensions == null ? null : (String) extensions.get("code");
    }

    @Value
    @Builder
    @Jacksonized
    public static class Location {

        Integer line;
        Integer column;
    }
}
