package com.flamingo.qa.clients;

import com.flamingo.qa.config.RestAssuredConfigurator;
import com.flamingo.qa.endpoints.GraphQlEndpoints;
import com.flamingo.qa.http.request.HttpMethod;
import com.flamingo.qa.http.response.ResponseWrapper;
import com.flamingo.qa.pojo.graphql.GraphQlRequest;
import com.flamingo.qa.util.ResourceReader;
import io.qameta.allure.Step;

import java.util.Map;

public class GraphQlApiClient extends BaseApiClient<GraphQlEndpoints> {

    private static final String DOCUMENT_DIRECTORY = "graphql/";

    public GraphQlApiClient() {
        super(RestAssuredConfigurator::graphqlSpec, new GraphQlEndpoints());
    }

    @Step("Run GraphQL document {document} with variables {variables}")
    public ResponseWrapper query(String document, Map<String, Object> variables) {
        return send(GraphQlRequest.builder()
                .query(ResourceReader.readString(DOCUMENT_DIRECTORY + document))
                .variables(variables)
                .build());
    }

    @Step("Run GraphQL document {document}")
    public ResponseWrapper query(String document) {
        return query(document, Map.of());
    }

    private ResponseWrapper send(GraphQlRequest request) {
        return request(HttpMethod.POST, endpoints.getQueryUri(), request);
    }
}
