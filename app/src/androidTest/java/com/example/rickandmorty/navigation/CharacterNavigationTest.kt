package com.example.rickandmorty.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextReplacement
import com.example.rickandmorty.domain.model.Character
import com.example.rickandmorty.domain.model.CharacterFilters
import com.example.rickandmorty.domain.model.CharacterPage
import com.example.rickandmorty.domain.model.Episode
import com.example.rickandmorty.domain.repository.CharacterPageResult
import com.example.rickandmorty.domain.repository.CharacterRepository
import com.example.rickandmorty.domain.repository.CharacterResult
import com.example.rickandmorty.domain.repository.EpisodeResult
import com.example.rickandmorty.ui.theme.RickAndMortyAppTheme
import org.junit.Rule
import org.junit.Test

class CharacterNavigationTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun opensDetailByIdAndReturnsWithSearchAndScrollPosition() {
        composeRule.setContent {
            RickAndMortyAppTheme(dynamicColor = false) {
                AuthenticatedNavHost(
                    characterRepository = FakeNavigationRepository(),
                    onLogout = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNode(hasText("Buscar por nombre") and hasSetTextAction())
            .performTextReplacement("Character")
        composeRule.mainClock.advanceTimeBy(400)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("character_list").performScrollToIndex(14)
        composeRule.onNodeWithTag("character_card_15")
            .assertIsDisplayed()
            .performClick()

        composeRule.onNodeWithContentDescription("Volver al listado").assertIsDisplayed()
        composeRule.onNodeWithTag("character_detail_marker_15", useUnmergedTree = true)
            .assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Volver al listado").performClick()
        composeRule.onNode(hasText("Buscar por nombre") and hasSetTextAction())
            .assertTextContains("Character")
        composeRule.onNodeWithTag("character_card_15").assertIsDisplayed()
    }

    private class FakeNavigationRepository : CharacterRepository {
        override suspend fun getCharacters(
            page: Int,
            filters: CharacterFilters,
        ): CharacterPageResult = CharacterPageResult.Success(
            CharacterPage(
                characters = characters,
                currentPage = 1,
                totalCharacters = characters.size,
                totalPages = 1,
                nextPageUrl = null,
                previousPageUrl = null,
                isLastPage = true,
            ),
        )

        override suspend fun getCharacter(id: Int): CharacterResult =
            CharacterResult.Success(characters.first { it.id == id })

        override suspend fun getEpisodes(ids: List<Int>): EpisodeResult =
            EpisodeResult.Success(listOf(Episode(1, "Pilot", "S01E01")))
    }

    private companion object {
        val characters = List(20) { index ->
            val id = index + 1
            Character(
                id = id,
                name = "Character $id",
                status = "Alive",
                species = "Human",
                type = "",
                gender = "Unknown",
                originName = "Earth",
                originUrl = "",
                locationName = "Citadel of Ricks",
                locationUrl = "",
                imageUrl = "https://example.test/$id.png",
                episodeUrls = listOf("https://example.test/episode/1"),
                resourceUrl = "",
                createdAt = "",
            )
        }
    }
}
