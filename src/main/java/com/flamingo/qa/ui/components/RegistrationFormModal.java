package com.flamingo.qa.ui.components;

import com.flamingo.qa.pojo.ui.Employee;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import lombok.Getter;

@Getter
public class RegistrationFormModal extends BaseComponent {

    private final Locator container = page.locator(".modal-content");
    private final Locator title = page.locator("#registration-form-modal");
    private final Locator firstName = page.locator("#firstName");
    private final Locator lastName = page.locator("#lastName");
    private final Locator email = page.locator("#userEmail");
    private final Locator age = page.locator("#age");
    private final Locator salary = page.locator("#salary");
    private final Locator department = page.locator("#department");
    private final Locator submitButton = page.locator("#submit");

    public RegistrationFormModal(Page page) {
        super(page);
    }

    public void fill(Employee employee) {
        firstName.fill(employee.firstName());
        lastName.fill(employee.lastName());
        email.fill(employee.email());
        age.fill(employee.age());
        salary.fill(employee.salary());
        department.fill(employee.department());
    }

    public void submit() {
        submitButton.click();
    }

    public void fillAndSubmit(Employee employee) {
        fill(employee);
        submit();
    }
}
