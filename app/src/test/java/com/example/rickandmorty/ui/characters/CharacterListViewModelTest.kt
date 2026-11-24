package com.example.rickandmorty.ui.characters

import com.example.rickandmorty.domain.model.Character
import com.example.rickandmorty.domain.model.CharacterPage
import com.example.rickandmorty.domain.repository.CharacterPageResult
import com.example.rickandmorty.domain.repository.CharacterRepository
import com.example.rickandmorty.domain.repository.CharacterRepositoryError
import kotlinx.coroutines.CancellationException
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CharacterListViewModelTest {

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
    fun `starts loading and exposes characters after initial success`() = runTest(dispatcher) {
        val repository = FakeCharacterRepository { successPage(page = 1, ids = listOf(1, 2)) }

        val viewModel = CharacterListViewModel(repository)

        assertTrue(viewModel.uiState.value.isInitialLoading)
        advanceUntilIdle()
        assertEquals(listOf(1, 2), viewModel.uiState.value.characters.map { it.id })
        assertFalse(viewModel.uiState.value.isInitialLoading)
        assertNull(viewModel.uiState.value.initialError)
    }

    @Test
    fun `retries the first page after an initial error`() = runTest(dispatcher) {
        var shouldFail = true
        val repository = FakeCharacterRepository {
            if (shouldFail) {
                failure(CharacterRepositoryError.Connection)
            } else {
                successPage(page = 1, ids = listOf(1))
            }
        }
        val viewModel = CharacterListViewModel(repository)
        advanceUntilIdle()
        assertEquals(CharacterListUiError.Connection, viewModel.uiState.value.initialError)

        shouldFail = false
        viewModel.retryInitialLoad()
        advanceUntilIdle()

        assertEquals(listOf(1), viewModel.uiState.value.characters.map { it.id })
        assertNull(viewModel.uiState.value.initialError)
        assertEquals(listOf(1, 1), repository.requestedPages)
    }

    @Test
    fun `loads another page and accumulates characters without duplicates`() = runTest(dispatcher) {
        val repository = FakeCharacterRepository { page ->
            when (page) {
                1 -> successPage(page = 1, ids = listOf(1, 2))
                else -> successPage(page = 2, ids = listOf(2, 3), isLastPage = true)
            }
        }
        val viewModel = CharacterListViewModel(repository)
        advanceUntilIdle()

        viewModel.loadNextPage()
        advanceUntilIdle()

        assertEquals(listOf(1, 2, 3), viewModel.uiState.value.characters.map { it.id })
        assertTrue(viewModel.uiState.value.isLastPage)
        assertEquals(listOf(1, 2), repository.requestedPages)
    }

    @Test
    fun `retries the same page after a pagination error`() = runTest(dispatcher) {
        var pageTwoAttempts = 0
        val repository = FakeCharacterRepository { page ->
            if (page == 1) {
                successPage(page = 1, ids = listOf(1))
            } else if (pageTwoAttempts++ == 0) {
                failure(CharacterRepositoryError.Http(500))
            } else {
                successPage(page = 2, ids = listOf(2), isLastPage = true)
            }
        }
        val viewModel = CharacterListViewModel(repository)
        advanceUntilIdle()
        viewModel.loadNextPage()
        advanceUntilIdle()
        assertEquals(CharacterListUiError.Http(500), viewModel.uiState.value.paginationError)

        viewModel.retryPagination()
        advanceUntilIdle()

        assertEquals(listOf(1, 2), viewModel.uiState.value.characters.map { it.id })
        assertNull(viewModel.uiState.value.paginationError)
        assertEquals(listOf(1, 2, 2), repository.requestedPages)
    }

    @Test
    fun `ignores simultaneous pagination requests`() = runTest(dispatcher) {
        val pageTwoResult = CompletableDeferred<CharacterPageResult>()
        val repository = FakeCharacterRepository { page ->
            if (page == 1) successPage(page = 1, ids = listOf(1)) else pageTwoResult.await()
        }
        val viewModel = CharacterListViewModel(repository)
        advanceUntilIdle()

        viewModel.loadNextPage()
        viewModel.loadNextPage()
        runCurrent()

        assertEquals(listOf(1, 2), repository.requestedPages)
        assertTrue(viewModel.uiState.value.isLoadingMore)
        pageTwoResult.complete(successPage(page = 2, ids = listOf(2), isLastPage = true))
        advanceUntilIdle()
    }

    @Test
    fun `does not request more data after the last page`() = runTest(dispatcher) {
        val repository = FakeCharacterRepository {
            successPage(page = 1, ids = listOf(1), isLastPage = true)
        }
        val viewModel = CharacterListViewModel(repository)
        advanceUntilIdle()

        viewModel.loadNextPage()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isLastPage)
        assertEquals(listOf(1), repository.requestedPages)
    }

    @Test
    fun `keeps loaded characters when pagination fails`() = runTest(dispatcher) {
        val repository = FakeCharacterRepository { page ->
            if (page == 1) {
                successPage(page = 1, ids = listOf(1, 2))
            } else {
                failure(CharacterRepositoryError.Serialization)
            }
        }
        val viewModel = CharacterListViewModel(repository)
        advanceUntilIdle()

        viewModel.loadNextPage()
        advanceUntilIdle()

        assertEquals(listOf(1, 2), viewModel.uiState.value.characters.map { it.id })
        assertEquals(CharacterListUiError.Serialization, viewModel.uiState.value.paginationError)
    }

    @Test
    fun `propagates cancellation without exposing it as an error`() = runTest(dispatcher) {
        val repository = FakeCharacterRepository { throw CancellationException("cancelled") }

        val viewModel = CharacterListViewModel(repository)
        runCurrent()

        assertFalse(viewModel.uiState.value.isInitialLoading)
        assertNull(viewModel.uiState.value.initialError)
        assertNull(viewModel.uiState.value.paginationError)
    }

    private class FakeCharacterRepository(
        private val response: suspend (page: Int) -> CharacterPageResult,
    ) : CharacterRepository {
        val requestedPages = mutableListOf<Int>()

        override suspend fun getCharacters(page: Int): CharacterPageResult {
            requestedPages += page
            return response(page)
        }
    }

    private companion object {
        fun successPage(
            page: Int,
            ids: List<Int>,
            isLastPage: Boolean = false,
        ) = CharacterPageResult.Success(
            CharacterPage(
                characters = ids.map(::character),
                currentPage = page,
                totalCharacters = ids.size,
                totalPages = if (isLastPage) page else page + 1,
                nextPageUrl = if (isLastPage) null else "https://example.test/character?page=${page + 1}",
                previousPageUrl = if (page == 1) null else "https://example.test/character?page=${page - 1}",
                isLastPage = isLastPage,
            ),
        )

        fun failure(error: CharacterRepositoryError) = CharacterPageResult.Failure(error)

        fun character(id: Int) = Character(
            id = id,
            name = "Character $id",
            status = "Alive",
            species = "Human",
            type = "",
            gender = "Unknown",
            originName = "Origin",
            originUrl = "",
            locationName = "Location",
            locationUrl = "",
            imageUrl = "https://example.test/$id.png",
            episodeUrls = emptyList(),
            resourceUrl = "https://example.test/character/$id",
            createdAt = "2026-01-01T00:00:00Z",
        )
    }
}
