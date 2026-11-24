package com.example.rickandmorty.ui.characters

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.rickandmorty.domain.model.Character
import com.example.rickandmorty.ui.theme.RickAndMortyAppTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CharacterListScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersCharacterInformation() {
        composeRule.setCharacterContent(
            CharacterListUiState(
                characters = listOf(character),
                isInitialLoading = false,
            ),
        )

        composeRule.onNodeWithText("Rick Sanchez").assertIsDisplayed()
        composeRule.onNodeWithText("Estado: Alive").assertIsDisplayed()
        composeRule.onNodeWithText("Especie: Human").assertIsDisplayed()
    }

    @Test
    fun rendersInitialLoadingState() {
        composeRule.setCharacterContent(CharacterListUiState())

        composeRule.onNodeWithText("Cargando personajes…").assertIsDisplayed()
    }

    @Test
    fun rendersEmptyState() {
        composeRule.setCharacterContent(
            CharacterListUiState(isInitialLoading = false),
        )

        composeRule.onNodeWithText("No hay personajes").assertIsDisplayed()
    }

    @Test
    fun rendersInitialErrorAndInvokesRetry() {
        var retried = false
        composeRule.setCharacterContent(
            uiState = CharacterListUiState(
                isInitialLoading = false,
                initialError = CharacterListUiError.Connection,
            ),
            onRetryInitialLoad = { retried = true },
        )

        composeRule.onNodeWithText("No pudimos cargar los personajes").assertIsDisplayed()
        composeRule.onNodeWithText("Reintentar").performClick()

        assertTrue(retried)
    }

    @Test
    fun rendersPaginationLoadingWithoutHidingCharacters() {
        composeRule.setCharacterContent(
            CharacterListUiState(
                characters = listOf(character),
                isInitialLoading = false,
                isLoadingMore = true,
            ),
        )

        composeRule.onNodeWithText("Rick Sanchez").assertIsDisplayed()
        composeRule.onNodeWithText("Cargando más personajes…").assertIsDisplayed()
    }

    @Test
    fun rendersPaginationErrorAndInvokesRetry() {
        var retried = false
        composeRule.setCharacterContent(
            uiState = CharacterListUiState(
                characters = listOf(character),
                isInitialLoading = false,
                paginationError = CharacterListUiError.Http(500),
            ),
            onRetryPagination = { retried = true },
        )

        composeRule.onNodeWithText("Rick Sanchez").assertIsDisplayed()
        composeRule.onNodeWithText("No pudimos cargar más personajes").assertIsDisplayed()
        composeRule.onNodeWithText("Reintentar").performClick()

        assertTrue(retried)
    }

    @Test
    fun rendersEndOfPaginationMessage() {
        composeRule.setCharacterContent(
            CharacterListUiState(
                characters = listOf(character),
                isInitialLoading = false,
                isLastPage = true,
            ),
        )

        composeRule.onNodeWithText("Has llegado al final de la lista.").assertIsDisplayed()
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.setCharacterContent(
        uiState: CharacterListUiState,
        onRetryInitialLoad: () -> Unit = {},
        onRetryPagination: () -> Unit = {},
    ) {
        setContent {
            RickAndMortyAppTheme(dynamicColor = false) {
                CharacterListContent(
                    uiState = uiState,
                    onLoadNextPage = {},
                    onRetryInitialLoad = onRetryInitialLoad,
                    onRetryPagination = onRetryPagination,
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
            originName = "Earth",
            originUrl = "",
            locationName = "Earth",
            locationUrl = "",
            imageUrl = "https://example.test/rick.png",
            episodeUrls = emptyList(),
            resourceUrl = "",
            createdAt = "",
        )
    }
}
