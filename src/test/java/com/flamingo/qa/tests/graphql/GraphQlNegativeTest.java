package com.flamingo.qa.tests.graphql;

import com.flamingo.qa.base.BaseGraphQlTest;
import com.flamingo.qa.pojo.graphql.GraphQlError;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.flamingo.qa.http.response.StatusCode.STATUS_200_OK;
import static com.flamingo.qa.http.response.StatusCode.STATUS_400_BAD_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;

@Feature("GraphQL error contracts")
@DisplayName("GraphQL negative queries")
class GraphQlNegativeTest extends BaseGraphQlTest {

    private static final String CHARACTER_BY_ID = "character-by-id.graphql";
    private static final String MALFORMED_QUERY = "malformed-query.graphql";
    private static final String UNKNOWN_FIELD = "unknown-field.graphql";

    @Test
    @Story("A query for something that does not exist is not an error")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("returns 200 with a null entity, and no errors array, for an unknown id")
    void shouldReturnNullDataForNonExistentId() {
        graphQlClient.query(CHARACTER_BY_ID, Map.of("id", "999999"))
                .verify()
                .hasStatusCode(STATUS_200_OK)
                .hasJsonField("data")
                .hasNoJsonField("data.character")
                .hasNoJsonField("errors");
    }

    @Test
    @Story("An invalid document is rejected")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("returns 400 with an errors array, and no data, for a malformed query")
    void shouldReturnSyntaxErrorForMalformedQuery() {
        List<GraphQlError> errors = graphQlClient.query(MALFORMED_QUERY)
                .verify()
                .hasStatusCode(STATUS_400_BAD_REQUEST)
                .hasNoJsonField("data")
                .and().asListAt("errors", GraphQlError.class);

        assertThat(errors).isNotEmpty();
        assertThat(errors.getFirst().message())
                .as("the wording belongs to the CDN that rejects the document, so only its "
                        + "presence is contract")
                .isNotBlank();
    }

    @Test
    @Story("An invalid document is rejected")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("returns a validation error naming the unknown field and its type")
    void shouldReturnValidationErrorForUnknownField() {
        List<GraphQlError> errors = graphQlClient.query(UNKNOWN_FIELD, Map.of("id", "1"))
                .verify()
                .hasStatusCode(STATUS_400_BAD_REQUEST)
                .hasNoJsonField("data")
                .and().asListAt("errors", GraphQlError.class);

        assertThat(errors).hasSize(1);
        assertThat(errors.getFirst().message())
                .isEqualTo("""
                        Cannot query field "nopeNotAField" on type "Character".""");
        assertThat(errors.getFirst().extensionCode()).isEqualTo("GRAPHQL_VALIDATION_FAILED");
        assertThat(errors.getFirst().locations())
                .as("the error should point at where in the document the field appears")
                .isNotEmpty();
    }

    @Test
    @Story("An invalid document is rejected")
    @Severity(SeverityLevel.MINOR)
    @DisplayName("rejects a query whose required variable was not supplied")
    void shouldReturnErrorWhenRequiredVariableIsMissing() {
        List<GraphQlError> errors = graphQlClient.query(CHARACTER_BY_ID, Map.of())
                .verify()
                .hasStatusCode(STATUS_400_BAD_REQUEST)
                .hasNoJsonField("data")
                .and().asListAt("errors", GraphQlError.class);

        assertThat(errors).hasSize(1);
        assertThat(errors.getFirst().message())
                .isEqualTo("""
                        Variable "$id" of required type "ID!" was not provided.""");
        assertThat(errors.getFirst().extensionCode())
                .as("same class of failure as an unknown field, yet labelled a server error")
                .isEqualTo("INTERNAL_SERVER_ERROR");
    }
}
