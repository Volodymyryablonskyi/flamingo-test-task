package com.flamingo.qa.ui.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import lombok.Getter;

/**
 * The student registration form at {@code /automation-practice-form}.
 *
 * <p>Locators are fields, not methods. A Playwright {@link Locator} is a lazy description of
 * how to find an element, resolved afresh on every action, so holding one costs nothing and
 * cannot go stale the way a Selenium {@code WebElement} would.
 *
 * <p>Locators prefer the accessible name over a CSS id wherever DemoQA provides one, so a
 * markup change that keeps the form usable does not break the tests.
 */
@Getter
public class PracticeFormPage extends BasePage {

    private static final String PATH = "/automation-practice-form";

    private final Locator heading = page.getByText("Student Registration Form");
    private final Locator firstName = page.getByPlaceholder("First Name");
    private final Locator lastName = page.getByPlaceholder("Last Name");
    private final Locator submitButton = page.locator("#submit");

    public PracticeFormPage(Page page) {
        super(page);
    }

    @Override
    protected String path() {
        return PATH;
    }
}
