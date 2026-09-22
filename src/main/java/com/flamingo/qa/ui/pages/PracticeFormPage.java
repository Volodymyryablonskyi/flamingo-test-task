package com.flamingo.qa.ui.pages;

import com.flamingo.qa.pojo.ui.Student;
import com.flamingo.qa.ui.components.SubmissionModal;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import lombok.Getter;

import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Getter
public class PracticeFormPage extends BasePage {

    private static final String PATH = "/automation-practice-form";
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH);
    private static final Locator.GetByTextOptions EXACT = new Locator.GetByTextOptions().setExact(true);

    private final Locator userForm = page.locator("#userForm");
    private final Locator invalidFields = page.locator("#userForm input:invalid");
    private final Locator firstName = page.locator("#firstName");
    private final Locator lastName = page.locator("#lastName");
    private final Locator email = page.locator("#userEmail");
    private final Locator mobile = page.locator("#userNumber");
    private final Locator genderGroup = page.locator("#genterWrapper");
    private final Locator hobbiesGroup = page.locator("#hobbiesWrapper");
    private final Locator dateOfBirthInput = page.locator("#dateOfBirthInput");
    private final Locator monthSelect = page.locator(".react-datepicker__month-select");
    private final Locator yearSelect = page.locator(".react-datepicker__year-select");
    private final Locator subjectsContainer = page.locator("#subjectsContainer");
    private final Locator subjectsInput = page.locator("#subjectsInput");
    private final Locator uploadPicture = page.locator("#uploadPicture");
    private final Locator currentAddress = page.locator("#currentAddress");
    private final Locator stateContainer = page.locator("#state");
    private final Locator stateInput = page.locator("#react-select-3-input");
    private final Locator cityContainer = page.locator("#city");
    private final Locator cityInput = page.locator("#react-select-4-input");
    private final Locator submitButton = page.locator("#submit");

    public PracticeFormPage(Page page) {
        super(page);
    }

    @Override
    protected String path() {
        return PATH;
    }

    public Locator gender(String label) {
        return genderGroup.getByText(label, EXACT);
    }

    public Locator hobby(String label) {
        return hobbiesGroup.getByText(label, EXACT);
    }

    @Step("Fill the registration form for {student.firstName} {student.lastName}")
    public PracticeFormPage fill(Student student) {
        firstName.fill(student.firstName());
        lastName.fill(student.lastName());
        email.fill(student.email());
        gender(student.gender()).click();
        mobile.fill(student.mobile());
        selectDateOfBirth(student);
        selectSubject(student.subject());
        hobby(student.hobby()).click();
        uploadPicture.setInputFiles(Path.of("src/test/resources/upload/" + student.pictureName()));
        currentAddress.fill(student.currentAddress());
        selectStateAndCity(student);
        return this;
    }

    @Step("Submit the registration form")
    public SubmissionModal submit() {
        scrollIntoView(submitButton);
        submitButton.click();
        return new SubmissionModal(page);
    }

    @Step("Submit the registration form expecting it to be rejected")
    public PracticeFormPage submitExpectingRejection() {
        scrollIntoView(submitButton);
        submitButton.click();
        return this;
    }

    public SubmissionModal fillAndSubmit(Student student) {
        return fill(student).submit();
    }

    private void selectDateOfBirth(Student student) {
        dateOfBirthInput.click();
        monthSelect.selectOption(student.dateOfBirth().format(MONTH));
        yearSelect.selectOption(String.valueOf(student.dateOfBirth().getYear()));
        page.locator(".react-datepicker__day--%03d:not(.react-datepicker__day--outside-month)"
                .formatted(student.dateOfBirth().getDayOfMonth())).click();
    }

    private void selectSubject(String subject) {
        subjectsInput.pressSequentially(subject);
        option(subjectsContainer, subject).click();
    }

    private void selectStateAndCity(Student student) {
        stateContainer.click();
        stateInput.pressSequentially(student.state());
        option(stateContainer, student.state()).click();

        cityContainer.click();
        cityInput.pressSequentially(student.city());
        option(cityContainer, student.city()).click();
    }

    private static Locator option(Locator container, String name) {
        return container.getByRole(AriaRole.OPTION,
                new Locator.GetByRoleOptions().setName(name).setExact(true));
    }
}
