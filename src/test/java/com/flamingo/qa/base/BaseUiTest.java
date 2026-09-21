package com.flamingo.qa.base;

import com.flamingo.qa.extensions.PlaywrightExtension;
import com.flamingo.qa.extensions.RequiresService;
import com.flamingo.qa.extensions.SystemUnderTest;
import com.microsoft.playwright.Page;
import io.qameta.allure.Epic;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;

@Epic("UI - DemoQA")
@Tag("ui")
@Tag("regression")
@RequiresService(SystemUnderTest.DEMOQA)
@ExtendWith(PlaywrightExtension.class)
public abstract class BaseUiTest {

    protected Page page() {
        return PlaywrightExtension.page();
    }
}
