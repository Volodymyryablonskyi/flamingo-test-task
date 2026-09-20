package com.flamingo.qa.base;

import com.flamingo.qa.clients.GraphQlApiClient;
import com.flamingo.qa.extensions.RequiresService;
import com.flamingo.qa.extensions.SystemUnderTest;
import io.qameta.allure.Epic;
import org.junit.jupiter.api.Tag;

@Epic("GraphQL API")
@Tag("graphql")
@RequiresService(SystemUnderTest.GRAPHQL)
public abstract class BaseGraphQlTest extends BaseApiTest {

    protected final GraphQlApiClient graphQlClient = new GraphQlApiClient();
}
