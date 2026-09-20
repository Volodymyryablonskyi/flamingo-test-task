package com.flamingo.qa.tests;

import com.flamingo.qa.core.extension.RequiresService;
import com.flamingo.qa.core.extension.SystemUnderTest;
import io.qameta.allure.Epic;
import org.junit.jupiter.api.Tag;

/**
 * Base class for the GraphQL tests.
 *
 * <p>Carries both tags: {@code graphql} for running them on their own, and {@code api}
 * inherited from {@link BaseApiTest} because the brief frames GraphQL as part of the API
 * exercise - so {@code mvn test -Dgroups="api"} runs them too.
 */
@Epic("GraphQL API")
@Tag("graphql")
@RequiresService(SystemUnderTest.GRAPHQL)
public abstract class BaseGraphQlTest extends BaseApiTest {
}
