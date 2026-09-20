package com.flamingo.qa.tests.graphql;

import com.flamingo.qa.base.BaseGraphQlTest;
import com.flamingo.qa.pojo.graphql.Character;
import com.flamingo.qa.pojo.graphql.CharactersPage;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.flamingo.qa.http.response.StatusCode.STATUS_200_OK;
import static org.assertj.core.api.Assertions.assertThat;

@Feature("GraphQL queries")
@DisplayName("GraphQL positive queries")
class CharacterQueryTest extends BaseGraphQlTest {

    private static final String CHARACTERS_BY_PAGE = "characters-by-page.graphql";
    private static final String CHARACTER_BY_ID = "character-by-id.graphql";
    private static final String CHARACTER_WITH_EPISODES = "character-with-episodes.graphql";

    private static final int PAGE_SIZE = 20;

    @Test
    @Tag("smoke")
    @Story("A collection can be paged")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("returns a page of characters with consistent pagination metadata")
    void shouldReturnPaginatedListOfCharacters() {
        CharactersPage page = charactersOnPage(1);

        assertThat(page.getResults())
                .hasSize(PAGE_SIZE)
                .doesNotContainNull()
                .allSatisfy(character -> assertThat(character.getName()).isNotBlank());

        assertThat(page.getInfo().getCount()).isPositive();
        assertThat(page.getInfo().getPages())
                .as("pages should be count divided by page size, rounded up")
                .isEqualTo((page.getInfo().getCount() + PAGE_SIZE - 1) / PAGE_SIZE);
        assertThat(page.getInfo().getPrev()).isNull();
        assertThat(page.getInfo().getNext()).isEqualTo(2);
    }

    @Test
    @Tag("smoke")
    @Story("A single entity can be fetched by id")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("returns a single character by id")
    void shouldReturnSingleCharacterById() {
        Character character = graphQlClient.query(CHARACTER_BY_ID, Map.of("id", "1"))
                .verify().hasStatusCode(STATUS_200_OK).hasNoJsonField("errors")
                .and().asPojoAt("data.character", Character.class);

        assertThat(character.getId()).isEqualTo("1");
        assertThat(character.getName()).isEqualTo("Rick Sanchez");
        assertThat(character.getStatus()).isEqualTo("Alive");
        assertThat(character.getSpecies()).isEqualTo("Human");
    }

    @Test
    @Story("A collection can be paged")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("pages through the collection using a GraphQL variable")
    void shouldPaginateUsingGraphQlVariables() {
        List<String> firstPage = idsOf(charactersOnPage(1));
        List<String> secondPage = idsOf(charactersOnPage(2));

        assertThat(secondPage)
                .as("a second page that repeated the first would make paging meaningless")
                .doesNotContainAnyElementsOf(firstPage)
                .hasSize(PAGE_SIZE);
    }

    @Test
    @Story("Related types resolve in one round trip")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("resolves a fragment and nested episodes across types")
    void shouldResolveNestedFieldsAcrossTypesUsingFragment() {
        Character character = graphQlClient.query(CHARACTER_WITH_EPISODES, Map.of("id", "1"))
                .verify().hasStatusCode(STATUS_200_OK).hasNoJsonField("errors")
                .and().asPojoAt("data.character", Character.class);

        assertThat(character.getName())
                .as("fields selected through the fragment")
                .isEqualTo("Rick Sanchez");
        assertThat(character.getSpecies()).isEqualTo("Human");

        assertThat(character.getEpisode())
                .as("episodes are a different type reached through the character")
                .isNotEmpty()
                .allSatisfy(episode -> {
                    assertThat(episode.getId()).isNotBlank();
                    assertThat(episode.getName()).isNotBlank();
                    assertThat(episode.getEpisode()).matches("S\\d{2}E\\d{2}");
                });
    }

    private CharactersPage charactersOnPage(int page) {
        return graphQlClient.query(CHARACTERS_BY_PAGE, Map.of("page", page))
                .verify().hasStatusCode(STATUS_200_OK).hasNoJsonField("errors")
                .and().asPojoAt("data.characters", CharactersPage.class);
    }

    private static List<String> idsOf(CharactersPage page) {
        return page.getResults().stream().map(Character::getId).toList();
    }
}
