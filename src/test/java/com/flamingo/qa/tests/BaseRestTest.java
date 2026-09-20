package com.flamingo.qa.tests;

import com.flamingo.qa.core.extension.RequiresService;
import com.flamingo.qa.core.extension.SystemUnderTest;
import io.qameta.allure.Epic;

/**
 * Base class for the Restful Booker REST tests.
 *
 * <p>Its only job is to declare the dependency on Restful Booker once, so that a cold-started
 * or reset Heroku dyno skips these classes with a reason instead of failing them.
 */
@Epic("REST API - Restful Booker")
@RequiresService(SystemUnderTest.RESTFUL_BOOKER)
public abstract class BaseRestTest extends BaseApiTest {
}
