package com.flamingo.qa.tests.ui;

import com.flamingo.qa.base.BaseUiTest;
import com.flamingo.qa.ui.pages.PracticeFormPage;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@Feature("UI framework")
@DisplayName("Browser stack")
class UiFrameworkSmokeTest extends BaseUiTest {

    @Test
    @Tag("smoke")
    @Story("The browser stack reaches DemoQA and renders it")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("opens the practice form and renders its fields")
    void shouldOpenPracticeFormInABrowser() {
        PracticeFormPage form = new PracticeFormPage(page());

        form.open();

        assertThat(form.getHeading()).isVisible();
        assertThat(form.getFirstName()).isEditable();
        assertThat(form.getSubmitButton()).isVisible();
    }
}
