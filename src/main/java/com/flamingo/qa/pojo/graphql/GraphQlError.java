package com.flamingo.qa.pojo.graphql;

import java.util.List;
import java.util.Map;

public record GraphQlError(String message,
                           List<Location> locations,
                           Map<String, Object> extensions) {

    public String extensionCode() {
        return extensions == null ? null : (String) extensions.get("code");
    }

    public record Location(Integer line, Integer column) {
    }
}
