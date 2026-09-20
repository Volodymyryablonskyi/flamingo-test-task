package com.flamingo.qa.pojo.graphql;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

/**
 * The body of a GraphQL POST.
 *
 * <p>{@code variables} is a map that Jackson serialises, never string interpolation into
 * the query text: interpolation loses the server-side type checking that makes a typed
 * variable worth declaring, and it is how injection gets in.
 */
@Value
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GraphQlRequest {

    String query;
    Map<String, Object> variables;
    String operationName;
}
