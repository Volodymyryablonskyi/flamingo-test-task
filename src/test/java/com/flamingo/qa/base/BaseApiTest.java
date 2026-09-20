package com.flamingo.qa.base;

import io.restassured.RestAssured;
import io.restassured.parsing.Parser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;

/**
 * Shared setup for every HTTP-level test. Intentionally almost empty: cross-cutting
 * behaviour that needs a lifecycle hook lives in extensions, so it can be composed onto a
 * class rather than inherited whether it is wanted or not.
 */
@Tag("api")
public abstract class BaseApiTest {

    @BeforeAll
    static void configureRestAssured() {
        // Restful Booker returns text/plain for some error bodies; without a default parser
        // a negative test fails on the parsing rather than on the contract it checks.
        // Idempotent, so running it once per subclass under parallel execution is safe.
        RestAssured.defaultParser = Parser.JSON;
    }
}
