package com.flamingo.qa.pojo.graphql;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.Map;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GraphQlRequest(String query, Map<String, Object> variables, String operationName) {
}
