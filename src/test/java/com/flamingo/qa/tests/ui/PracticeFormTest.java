package com.flamingo.qa.tests.ui;

import com.flamingo.qa.base.BaseUiTest;
import com.flamingo.qa.data.StudentDataGenerator;
import com.flamingo.qa.pojo.ui.Student;
import com.flamingo.qa.ui.components.SubmissionModal;
import com.flamingo.qa.ui.pages.PracticeFormPage;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@Feature("Practice form")
@DisplayName("Student registration form")
class PracticeFormTest extends BaseUiTest {

    private PracticeFormPage form;

    @BeforeEach
    void openForm() {
        form = new PracticeFormPage(page());
        form.open();
    }

    @Test
    @Tag("smoke")
    @Story("A complete registration is accepted")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("submits every field and echoes them back in the confirmation modal")
    void shouldSubmitCompleteRegistrationAndShowSuccessModal() {
        Student student = StudentDataGenerator.completeStudent();

        SubmissionModal modal = form.fillAndSubmit(student);

        assertThat(modal.getTitle()).hasText("Thanks for submitting the form");
        Map<String, String> submitted = modal.submittedValues();

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(submitted.get("Student Name")).isEqualTo(student.fullName());
            soft.assertThat(submitted.get("Student Email")).isEqualTo(student.getEmail());
            soft.assertThat(submitted.get("Gender")).isEqualTo(student.getGender());
            soft.assertThat(submitted.get("Mobile")).isEqualTo(student.getMobile());
            soft.assertThat(submitted.get("Date of Birth")).isEqualTo("15 June,1995");
            soft.assertThat(submitted.get("Subjects")).isEqualTo(student.getSubject());
            soft.assertThat(submitted.get("Hobbies")).isEqualTo(student.getHobby());
            soft.assertThat(submitted.get("Picture")).isEqualTo(student.getPictureName());
            soft.assertThat(submitted.get("Address")).isEqualTo(student.getCurrentAddress());
            soft.assertThat(submitted.get("State and City"))
                    .isEqualTo(student.getState() + " " + student.getCity());
        });
    }

    @Test
    @Story("An incomplete registration is refused")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("shows no confirmation modal when mandatory fields are empty")
    void shouldNotSubmitWhenMandatoryFieldsAreEmpty() {
        form.submitExpectingRejection();

        assertThat(new SubmissionModal(page()).getContainer()).isHidden();
        assertThat(form.getUserForm()).hasClass("was-validated");
        assertThat(form.getInvalidFields()).not().hasCount(0);
    }

    @ParameterizedTest(name = "mobile '{0}'")
    @ValueSource(strings = {"123", "abcdefghij", ""})
    @Story("An incomplete registration is refused")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("refuses a mobile number that is not ten digits")
    void shouldRejectInvalidMobileNumbers(String mobile) {
        Student student = StudentDataGenerator.completeStudent().toBuilder().mobile(mobile).build();

        form.fill(student).submitExpectingRejection();

        assertThat(new SubmissionModal(page()).getContainer()).isHidden();
    }
}
