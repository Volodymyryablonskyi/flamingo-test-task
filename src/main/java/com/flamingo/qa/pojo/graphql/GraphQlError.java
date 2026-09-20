package com.flamingo.qa.pojo.graphql;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Map;

@Value
@Builder
@Jacksonized
public class GraphQlError {

    String message;
    List<Location> locations;
    Map<String, Object> extensions;

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
