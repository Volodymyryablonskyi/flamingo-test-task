package com.flamingo.qa.ui.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import lombok.Getter;

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
