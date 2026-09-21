package com.flamingo.qa.ui.components;

import com.flamingo.qa.pojo.ui.Employee;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import lombok.Getter;

@Getter
public class RegistrationFormModal {

    private final Locator container;
    private final Locator title;
    private final Locator firstName;
    private final Locator lastName;
    private final Locator email;
    private final Locator age;
    private final Locator salary;
    private final Locator department;
    private final Locator submitButton;

    public RegistrationFormModal(Page page) {
        this.container = page.locator(".modal-content");
        this.title = page.locator("#registration-form-modal");
        this.firstName = page.locator("#firstName");
        this.lastName = page.locator("#lastName");
        this.email = page.locator("#userEmail");
        this.age = page.locator("#age");
        this.salary = page.locator("#salary");
        this.department = page.locator("#department");
        this.submitButton = page.locator("#submit");
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
