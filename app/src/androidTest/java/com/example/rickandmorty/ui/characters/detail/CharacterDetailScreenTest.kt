package com.example.rickandmorty.ui.characters.detail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import com.example.rickandmorty.domain.model.Character
import com.example.rickandmorty.domain.model.Episode
import com.example.rickandmorty.ui.characters.CharacterListUiError
import com.example.rickandmorty.ui.theme.RickAndMortyAppTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CharacterDetailScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersCharacterAndOrderedEpisodes() {
        composeRule.setDetailContent(
            CharacterDetailUiState(
                character = character,
                episodes = episodes,
                isCharacterLoading = false,
            ),
        )

        composeRule.onNodeWithTag("character_detail_list").performScrollToIndex(0)
        composeRule.onNodeWithText("Rick Sanchez").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Estado: Alive").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("character_detail_list").performScrollToIndex(2)
        composeRule.onNodeWithText("Citadel of Ricks").assertIsDisplayed()
        composeRule.onNodeWithTag("character_detail_list").performScrollToIndex(4)
        composeRule.onNodeWithText("S01E01").assertIsDisplayed()
        composeRule.onNodeWithText("Pilot").assertIsDisplayed()
        composeRule.onNodeWithTag("character_detail_list").performScrollToIndex(5)
        composeRule.onNodeWithText("S01E02").assertIsDisplayed()
    }

    @Test
    fun keepsCharacterVisibleWhenEpisodesFailAndRetries() {
        var retried = false
        composeRule.setDetailContent(
            uiState = CharacterDetailUiState(
                character = character,
                isCharacterLoading = false,
                episodeError = CharacterListUiError.Connection,
            ),
            onRetryEpisodes = { retried = true },
        )

        composeRule.onNodeWithTag("character_detail_list").performScrollToIndex(0)
        composeRule.onNodeWithText("Rick Sanchez").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("character_detail_list").performScrollToIndex(4)
        composeRule.onNodeWithText("El personaje está disponible, pero sus episodios no")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Reintentar").performClick()

        assertTrue(retried)
    }

    @Test
    fun rendersLoadingState() {
        composeRule.setDetailContent(CharacterDetailUiState())
        composeRule.onNodeWithText("Abriendo portal al personaje…").assertIsDisplayed()
    }

    @Test
    fun rendersEmptyEpisodesState() {
        composeRule.setDetailContent(
            CharacterDetailUiState(
                character = character.copy(episodeUrls = emptyList()),
                isCharacterLoading = false,
            ),
        )
        composeRule.onNodeWithTag("character_detail_list").performScrollToIndex(4)
        composeRule.onNodeWithText("Sin episodios registrados").assertIsDisplayed()
    }

    @Test
    fun backAndInitialRetryActionsAreForwarded() {
        var wentBack = false
        var retried = false
        composeRule.setDetailContent(
            uiState = CharacterDetailUiState(
                isCharacterLoading = false,
                characterError = CharacterListUiError.Http(404),
            ),
            onBack = { wentBack = true },
            onRetryCharacter = { retried = true },
        )

        composeRule.onNodeWithContentDescription("Volver al listado").performClick()
        composeRule.onNodeWithText("Reintentar").performClick()

        assertTrue(wentBack)
        assertTrue(retried)
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.setDetailContent(
        uiState: CharacterDetailUiState,
        onBack: () -> Unit = {},
        onRetryCharacter: () -> Unit = {},
        onRetryEpisodes: () -> Unit = {},
    ) {
        setContent {
            RickAndMortyAppTheme(dynamicColor = false) {
                CharacterDetailContent(
                    uiState = uiState,
                    onBack = onBack,
                    onRetryCharacter = onRetryCharacter,
                    onRetryEpisodes = onRetryEpisodes,
                )
            }
        }
    }

    private companion object {
        val character = Character(
            id = 1,
            name = "Rick Sanchez",
            status = "Alive",
            species = "Human",
            type = "",
            gender = "Male",
            originName = "Earth (C-137)",
            originUrl = "",
            locationName = "Citadel of Ricks",
            locationUrl = "",
            imageUrl = "https://example.test/rick.png",
            episodeUrls = listOf("https://example.test/episode/1"),
            resourceUrl = "",
            createdAt = "",
        )
        val episodes = listOf(
            Episode(1, "Pilot", "S01E01"),
            Episode(2, "Lawnmower Dog", "S01E02"),
        )
    }
}
