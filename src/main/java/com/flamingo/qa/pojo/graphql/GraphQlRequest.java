package com.flamingo.qa.pojo.graphql;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

@Value
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GraphQlRequest {

    String query;
    Map<String, Object> variables;
    String operationName;
}
