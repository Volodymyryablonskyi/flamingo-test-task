package com.flamingo.qa.clients;

import com.flamingo.qa.config.RestAssuredConfigurator;
import com.flamingo.qa.endpoints.AuthEndpoints;
import com.flamingo.qa.http.request.HttpMethod;
import com.flamingo.qa.http.response.ResponseWrapper;
import com.flamingo.qa.pojo.auth.AuthRequest;
import io.qameta.allure.Step;

public class AuthApiClient extends BaseApiClient<AuthEndpoints> {

    public AuthApiClient() {
        super(RestAssuredConfigurator::restSpec, new AuthEndpoints());
    }

    @Step("Request an auth token for user '{username}'")
    public ResponseWrapper requestToken(String username, String password) {
        return request(HttpMethod.POST, endpoints.getAuthUri(),
                AuthRequest.builder().username(username).password(password).build());
    }
}
