package com.example.rickandmorty.ui.characters.detail

import com.example.rickandmorty.domain.model.Character
import com.example.rickandmorty.domain.model.CharacterFilters
import com.example.rickandmorty.domain.model.Episode
import com.example.rickandmorty.domain.repository.CharacterPageResult
import com.example.rickandmorty.domain.repository.CharacterRepository
import com.example.rickandmorty.domain.repository.CharacterRepositoryError
import com.example.rickandmorty.domain.repository.CharacterResult
import com.example.rickandmorty.domain.repository.EpisodeResult
import com.example.rickandmorty.ui.characters.CharacterListUiError
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CharacterDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads character then episodes successfully`() = runTest(dispatcher) {
        val repository = FakeDetailRepository()
        val viewModel = CharacterDetailViewModel(2, repository)

        assertTrue(viewModel.uiState.value.isCharacterLoading)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.character?.id)
        assertEquals(listOf(1, 2), state.episodes.map { it.id })
        assertFalse(state.isCharacterLoading)
        assertFalse(state.isEpisodesLoading)
        assertEquals(listOf(2), repository.characterRequests)
        assertEquals(listOf(listOf(1, 2)), repository.episodeRequests)
    }

    @Test
    fun `retries character after initial error`() = runTest(dispatcher) {
        var shouldFail = true
        val repository = FakeDetailRepository(
            characterResponse = {
                if (shouldFail) CharacterResult.Failure(CharacterRepositoryError.Connection)
                else CharacterResult.Success(character)
            },
        )
        val viewModel = CharacterDetailViewModel(2, repository)
        advanceUntilIdle()
        assertEquals(CharacterListUiError.Connection, viewModel.uiState.value.characterError)

        shouldFail = false
        viewModel.retryCharacter()
        advanceUntilIdle()

        assertSame(character, viewModel.uiState.value.character)
        assertNull(viewModel.uiState.value.characterError)
        assertEquals(listOf(2, 2), repository.characterRequests)
    }

    @Test
    fun `keeps character visible when episodes fail and retries episodes`() = runTest(dispatcher) {
        var shouldFail = true
        val repository = FakeDetailRepository(
            episodeResponse = {
                if (shouldFail) EpisodeResult.Failure(CharacterRepositoryError.Http(500))
                else EpisodeResult.Success(episodes)
            },
        )
        val viewModel = CharacterDetailViewModel(2, repository)
        advanceUntilIdle()

        assertSame(character, viewModel.uiState.value.character)
        assertEquals(CharacterListUiError.Http(500), viewModel.uiState.value.episodeError)
        assertTrue(viewModel.uiState.value.episodes.isEmpty())

        shouldFail = false
        viewModel.retryEpisodes()
        advanceUntilIdle()

        assertSame(character, viewModel.uiState.value.character)
        assertEquals(episodes, viewModel.uiState.value.episodes)
        assertNull(viewModel.uiState.value.episodeError)
    }

    @Test
    fun `shows empty episodes without calling repository when character has none`() = runTest(dispatcher) {
        val repository = FakeDetailRepository(
            characterResponse = {
                CharacterResult.Success(character.copy(episodeUrls = emptyList()))
            },
        )

        val viewModel = CharacterDetailViewModel(2, repository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.episodes.isEmpty())
        assertFalse(viewModel.uiState.value.isEpisodesLoading)
        assertTrue(repository.episodeRequests.isEmpty())
    }

    @Test
    fun `does not duplicate episode requests while one is active`() = runTest(dispatcher) {
        val deferred = CompletableDeferred<EpisodeResult>()
        val repository = FakeDetailRepository(episodeResponse = { deferred.await() })
        val viewModel = CharacterDetailViewModel(2, repository)
        runCurrent()
        assertTrue(viewModel.uiState.value.isEpisodesLoading)

        viewModel.retryEpisodes()
        runCurrent()

        assertEquals(1, repository.episodeRequests.size)
        deferred.complete(EpisodeResult.Success(episodes))
        advanceUntilIdle()
    }

    private class FakeDetailRepository(
        private val characterResponse: suspend (Int) -> CharacterResult = {
            CharacterResult.Success(character)
        },
        private val episodeResponse: suspend (List<Int>) -> EpisodeResult = {
            EpisodeResult.Success(episodes)
        },
    ) : CharacterRepository {
        val characterRequests = mutableListOf<Int>()
        val episodeRequests = mutableListOf<List<Int>>()

        override suspend fun getCharacters(
            page: Int,
            filters: CharacterFilters,
        ): CharacterPageResult = error("Not used by detail tests")

        override suspend fun getCharacter(id: Int): CharacterResult {
            characterRequests += id
            return characterResponse(id)
        }

        override suspend fun getEpisodes(ids: List<Int>): EpisodeResult {
            episodeRequests += ids
            return episodeResponse(ids)
        }
    }

    private companion object {
        val character = Character(
            id = 2,
            name = "Morty Smith",
            status = "Alive",
            species = "Human",
            type = "",
            gender = "Male",
            originName = "unknown",
            originUrl = "",
            locationName = "Citadel of Ricks",
            locationUrl = "https://rickandmortyapi.com/api/location/3",
            imageUrl = "https://rickandmortyapi.com/api/character/avatar/2.jpeg",
            episodeUrls = listOf(
                "https://rickandmortyapi.com/api/episode/1",
                "https://rickandmortyapi.com/api/episode/2",
            ),
            resourceUrl = "https://rickandmortyapi.com/api/character/2",
            createdAt = "2017-11-04T18:50:21.651Z",
        )
        val episodes = listOf(
            Episode(1, "Pilot", "S01E01"),
            Episode(2, "Lawnmower Dog", "S01E02"),
        )
    }
}
