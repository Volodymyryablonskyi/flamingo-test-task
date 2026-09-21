package com.flamingo.qa.ui.pages;

import com.flamingo.qa.pojo.ui.Employee;
import com.flamingo.qa.ui.components.RegistrationFormModal;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import lombok.Getter;

import java.util.List;

@Getter
public class WebTablesPage extends BasePage {

    private static final String PATH = "/webtables";

    private final Locator addRecordButton = page.locator("#addNewRecordButton");
    private final Locator searchBox = page.locator("#searchBox");
    private final Locator headers = page.locator("table thead th");
    private final Locator rows = page.locator("table tbody tr");
    private final Locator pageSizeSelect = page.locator("select.form-control");
    private final Locator pageIndicator = page.locator(".col-auto strong");
    private final Locator previousPageButton = pageButton("Previous");
    private final Locator nextPageButton = pageButton("Next");

    public WebTablesPage(Page page) {
        super(page);
    }

    @Override
    protected String path() {
        return PATH;
    }

    private Locator pageButton(String name) {
        return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(name).setExact(true));
    }

    public Locator rowContaining(String text) {
        return rows.filter(new Locator.FilterOptions().setHasText(text));
    }

    public List<String> cellsOf(String text) {
        return rowContaining(text).locator("td").allInnerTexts().stream()
                .map(String::trim)
                .toList();
    }

    @Step("Open the registration form")
    public RegistrationFormModal openRegistrationForm() {
        addRecordButton.click();
        return new RegistrationFormModal(page);
    }

    @Step("Add {employee.firstName} {employee.lastName} to the table")
    public WebTablesPage addRecord(Employee employee) {
        openRegistrationForm().fillAndSubmit(employee);
        return this;
    }

    @Step("Edit the record containing '{text}'")
    public RegistrationFormModal editRecordContaining(String text) {
        rowContaining(text).locator("[id^='edit-record']").click();
        return new RegistrationFormModal(page);
    }

    @Step("Delete the record containing '{text}'")
    public WebTablesPage deleteRecordContaining(String text) {
        rowContaining(text).locator("[id^='delete-record']").click();
        return this;
    }

    @Step("Search for '{term}'")
    public WebTablesPage search(String term) {
        searchBox.fill(term);
        return this;
    }

    @Step("Show {size} rows per page")
    public WebTablesPage showRowsPerPage(int size) {
        pageSizeSelect.selectOption(String.valueOf(size));
        return this;
    }

    @Step("Go to the next page")
    public WebTablesPage goToNextPage() {
        nextPageButton.click();
        return this;
    }
}
