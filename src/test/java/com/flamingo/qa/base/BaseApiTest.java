package com.flamingo.qa.base;

import io.restassured.RestAssured;
import io.restassured.parsing.Parser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;

@Tag("api")
@Tag("regression")
public abstract class BaseApiTest {

    @BeforeAll
    static void configureRestAssured() {
        RestAssured.defaultParser = Parser.JSON;
    }
}
