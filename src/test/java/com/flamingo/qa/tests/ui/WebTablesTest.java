package com.flamingo.qa.tests.ui;

import com.flamingo.qa.base.BaseUiTest;
import com.flamingo.qa.data.StudentDataGenerator;
import com.flamingo.qa.pojo.ui.Employee;
import com.flamingo.qa.ui.components.RegistrationFormModal;
import com.flamingo.qa.ui.pages.WebTablesPage;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@Feature("Web tables")
@DisplayName("Web tables CRUD")
class WebTablesTest extends BaseUiTest {

    private static final int SEEDED_ROWS = 3;
    private static final int DEFAULT_PAGE_SIZE = 10;

    private WebTablesPage table;

    @BeforeEach
    void openTable() {
        table = new WebTablesPage(page());
        table.open();
    }

    @Test
    @Tag("smoke")
    @Story("A record can be added")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("adds a record and shows every field in the new row")
    void shouldAddNewRecordToTable() {
        Employee employee = StudentDataGenerator.employee();

        table.addRecord(employee);

        assertThat(table.getRows()).hasCount(SEEDED_ROWS + 1);
        Assertions.assertThat(table.cellsOf(employee.getLastName()))
                .containsExactlyElementsOf(appendActionColumn(employee));
    }

    @Test
    @Story("A record can be edited")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("edits one field and leaves the others unchanged")
    void shouldEditExistingRecord() {
        Employee employee = StudentDataGenerator.employee();
        table.addRecord(employee);

        Employee edited = employee.toBuilder().department("Rewritten").build();
        table.editRecordContaining(employee.getLastName()).fillAndSubmit(edited);

        Assertions.assertThat(table.cellsOf(edited.getLastName()))
                .containsExactlyElementsOf(appendActionColumn(edited));
        assertThat(table.getRows()).hasCount(SEEDED_ROWS + 1);
    }

    @Test
    @Story("A record can be deleted")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("deletes a record and drops the row count")
    void shouldDeleteRecord() {
        Employee employee = StudentDataGenerator.employee();
        table.addRecord(employee);
        assertThat(table.getRows()).hasCount(SEEDED_ROWS + 1);

        table.deleteRecordContaining(employee.getLastName());

        assertThat(table.getRows()).hasCount(SEEDED_ROWS);
        assertThat(table.rowContaining(employee.getLastName())).hasCount(0);
    }

    @ParameterizedTest(name = "{0} matches {1} row(s)")
    @CsvSource({"Cierra, 1", "Legal, 1", "example.com, 3", "NoSuchRecordAnywhere, 0"})
    @Story("The table can be searched")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("filters rows by a search term")
    void shouldFilterRecordsBySearchTerm(String term, int expectedRows) {
        table.search(term);

        assertThat(table.getRows()).hasCount(expectedRows);
    }

    @Test
    @Story("The table is paginated")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("splits records across pages once they outgrow the page size")
    void shouldPaginateRecordsBeyondThePageSize() {
        int overflow = 1;
        IntStream.range(0, DEFAULT_PAGE_SIZE + overflow - SEEDED_ROWS)
                .forEach(i -> table.addRecord(StudentDataGenerator.employee()));

        assertThat(table.getRows()).hasCount(DEFAULT_PAGE_SIZE);
        assertThat(table.getPageIndicator()).hasText("1 of 2");
        assertThat(table.getPreviousPageButton()).isDisabled();

        table.goToNextPage();

        assertThat(table.getRows()).hasCount(overflow);
        assertThat(table.getPageIndicator()).hasText("2 of 2");
        assertThat(table.getNextPageButton()).isDisabled();

        table.showRowsPerPage(DEFAULT_PAGE_SIZE * 2);

        assertThat(table.getRows()).hasCount(DEFAULT_PAGE_SIZE + overflow);
        assertThat(table.getPageIndicator()).hasText("1 of 1");
    }

    @Test
    @Story("A record can be added")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("keeps the registration form open when required fields are empty")
    void shouldValidateRequiredFieldsInRegistrationForm() {
        RegistrationFormModal registration = table.openRegistrationForm();

        registration.submit();

        assertThat(registration.getContainer()).isVisible();
        assertThat(table.getRows()).hasCount(SEEDED_ROWS);
    }

    private static List<String> appendActionColumn(Employee employee) {
        return Stream.concat(employee.asRow().stream(), Stream.of("")).toList();
    }
}
