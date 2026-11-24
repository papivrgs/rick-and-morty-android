package com.example.rickandmorty.ui.characters

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import com.example.rickandmorty.domain.model.Character
import com.example.rickandmorty.domain.model.CharacterStatusFilter
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
        composeRule.onNodeWithText("Reintentar")
            .performClick()

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
        composeRule.onNodeWithText("No pudimos cargar más personajes")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Reintentar")
            .performScrollTo()
            .performClick()

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

        composeRule.onNodeWithText("Has llegado al final de la lista.")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun filterControlsForwardNameStatusAndFreeSpecies() {
        var searchedName = ""
        var selectedStatus = CharacterStatusFilter.All
        var species = ""
        val uiState = mutableStateOf(CharacterListUiState(isInitialLoading = false))
        composeRule.setContent {
            RickAndMortyAppTheme(dynamicColor = false) {
                CharacterListContent(
                    uiState = uiState.value,
                    onLoadNextPage = {},
                    onSearchQueryChanged = {
                        searchedName = it
                        uiState.value = uiState.value.copy(searchQuery = it)
                    },
                    onStatusSelected = {
                        selectedStatus = it
                        uiState.value = uiState.value.copy(selectedStatus = it)
                    },
                    onSpeciesChanged = {
                        species = it
                        uiState.value = uiState.value.copy(speciesQuery = it)
                    },
                )
            }
        }

        composeRule.onNode(hasText("Buscar por nombre") and hasSetTextAction())
            .performTextReplacement("Rick")
        composeRule.onNodeWithText("Alive").performClick()
        composeRule.onNode(hasText("Especie") and hasSetTextAction())
            .performTextReplacement("Human")
        composeRule.waitForIdle()

        assertTrue(searchedName.endsWith("Rick"))
        assertTrue(selectedStatus == CharacterStatusFilter.Alive)
        assertTrue(species.endsWith("Human"))
    }

    @Test
    fun filteredEmptyStateExplainsNoResultsAndClearsFilters() {
        var cleared = false
        composeRule.setCharacterContent(
            uiState = CharacterListUiState(
                searchQuery = "Nobody",
                isInitialLoading = false,
                isLastPage = true,
            ),
            onClearFilters = { cleared = true },
        )

        composeRule.onNodeWithText("Sin coincidencias en este universo").assertIsDisplayed()
        composeRule.onAllNodesWithText("Limpiar filtros")[0].performClick()

        assertTrue(cleared)
    }

    @Test
    fun keepsCharactersVisibleWhenRefreshFails() {
        composeRule.setCharacterContent(
            uiState = CharacterListUiState(
                characters = listOf(character),
                totalCharacters = 1,
                isInitialLoading = false,
                refreshError = CharacterListUiError.Connection,
            ),
        )

        composeRule.onNodeWithText("Rick Sanchez").assertIsDisplayed()
        composeRule.onNodeWithText("No pudimos actualizar los personajes")
            .assertIsDisplayed()
    }

    @Test
    fun scrollDirectionCollapsesAndRestoresFiltersWhileSearchStaysVisible() {
        composeRule.setCharacterContent(
            uiState = CharacterListUiState(
                characters = List(20) { index ->
                    character.copy(id = index + 1, name = "Personaje ${index + 1}")
                },
                totalCharacters = 20,
                isInitialLoading = false,
                isLastPage = true,
            ),
        )

        composeRule.onNodeWithTag("character_filter_panel").assertIsDisplayed()
        composeRule.onNodeWithTag("character_list").performTouchInput { swipeUp() }
        composeRule.waitForIdle()

        composeRule.onAllNodesWithTag("character_filter_panel").assertCountEquals(0)
        composeRule.onNodeWithTag("character_search_field").assertIsDisplayed()

        composeRule.onNodeWithTag("character_list").performTouchInput { swipeDown() }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("character_filter_panel").assertIsDisplayed()
    }

    @Test
    fun filterButtonTogglesPanelWithoutLosingActiveValues() {
        composeRule.setCharacterContent(
            uiState = CharacterListUiState(
                characters = listOf(character),
                selectedStatus = CharacterStatusFilter.Alive,
                speciesQuery = "Human",
                isInitialLoading = false,
                isLastPage = true,
            ),
        )

        composeRule.onNodeWithTag("character_filter_toggle").performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag("character_filter_panel").assertCountEquals(0)

        composeRule.onNodeWithTag("character_filter_toggle").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("character_filter_panel").assertIsDisplayed()
        composeRule.onNode(hasText("Human") and hasSetTextAction()).assertIsDisplayed()
        composeRule.onNodeWithText("Alive").assertIsDisplayed()
    }

    @Test
    fun logoutRequiresConfirmation() {
        var loggedOut = false
        composeRule.setCharacterContent(
            uiState = CharacterListUiState(
                characters = listOf(character),
                isInitialLoading = false,
            ),
            onLogout = { loggedOut = true },
        )

        composeRule.onNodeWithTag("character_logout_button").performClick()
        composeRule.onNodeWithText("¿Cerrar sesión?").assertIsDisplayed()
        composeRule.onNodeWithText("Cancelar").performClick()
        assertTrue(!loggedOut)

        composeRule.onNodeWithTag("character_logout_button").performClick()
        composeRule.onNodeWithText("Cerrar sesión").performClick()
        assertTrue(loggedOut)
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.setCharacterContent(
        uiState: CharacterListUiState,
        onRetryInitialLoad: () -> Unit = {},
        onRetryPagination: () -> Unit = {},
        onSearchQueryChanged: (String) -> Unit = {},
        onStatusSelected: (CharacterStatusFilter) -> Unit = {},
        onSpeciesChanged: (String) -> Unit = {},
        onClearFilters: () -> Unit = {},
        onLogout: () -> Unit = {},
    ) {
        setContent {
            RickAndMortyAppTheme(dynamicColor = false) {
                CharacterListContent(
                    uiState = uiState,
                    onLoadNextPage = {},
                    onRetryInitialLoad = onRetryInitialLoad,
                    onRetryPagination = onRetryPagination,
                    onSearchQueryChanged = onSearchQueryChanged,
                    onStatusSelected = onStatusSelected,
                    onSpeciesChanged = onSpeciesChanged,
                    onClearFilters = onClearFilters,
                    onLogout = onLogout,
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
