package com.flamingo.qa.ui.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * The student registration form at {@code /automation-practice-form}.
 *
 * <p>Locators prefer the accessible name over a CSS id wherever DemoQA provides one, so a
 * markup change that keeps the form usable does not break the tests.
 */
public class PracticeFormPage extends BasePage {

    private static final String PATH = "/automation-practice-form";

    public PracticeFormPage(Page page) {
        super(page);
    }

    @Override
    protected String path() {
        return PATH;
    }

    public Locator heading() {
        return page.getByText("Student Registration Form");
    }

    public Locator firstName() {
        return page.getByPlaceholder("First Name");
    }

    public Locator lastName() {
        return page.getByPlaceholder("Last Name");
    }

    public Locator submitButton() {
        return page.locator("#submit");
    }
}
