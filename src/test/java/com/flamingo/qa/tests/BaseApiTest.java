package com.flamingo.qa.tests;

import io.restassured.RestAssured;
import io.restassured.parsing.Parser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;

/**
 * Shared setup for every HTTP-level test, REST and GraphQL alike.
 *
 * <p>Intentionally almost empty. Cross-cutting behaviour that needs a lifecycle hook -
 * health checks, reporting, failure capture - lives in JUnit extensions instead, so it can
 * be composed onto a class with an annotation rather than inherited whether it is wanted or
 * not. What is left here is genuinely global REST Assured state.
 *
 * @see com.flamingo.qa.core.extension.RequiresService
 */
@Tag("api")
public abstract class BaseApiTest {

    @BeforeAll
    static void configureRestAssured() {
        // Restful Booker returns text/plain for some error bodies and HTML for others.
        // Without a default parser REST Assured refuses to path into them, and a negative
        // test fails on the parsing rather than on the contract it is checking.
        // Idempotent, so running it once per subclass under parallel execution is safe.
        RestAssured.defaultParser = Parser.JSON;
    }
}
