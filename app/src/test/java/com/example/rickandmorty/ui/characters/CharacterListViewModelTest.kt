package com.example.rickandmorty.ui.characters

import com.example.rickandmorty.domain.model.Character
import com.example.rickandmorty.domain.model.CharacterFilters
import com.example.rickandmorty.domain.model.CharacterPage
import com.example.rickandmorty.domain.model.CharacterStatusFilter
import com.example.rickandmorty.domain.repository.CharacterPageResult
import com.example.rickandmorty.domain.repository.CharacterRepository
import com.example.rickandmorty.domain.repository.CharacterRepositoryError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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

    @Test
    fun `debounces name search for 350 milliseconds`() = runTest(dispatcher) {
        val repository = FakeCharacterRepository { page -> successPage(page, listOf(page)) }
        val viewModel = CharacterListViewModel(repository)
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("Rick")
        advanceTimeBy(349)
        runCurrent()
        assertEquals(1, repository.requests.size)

        advanceTimeBy(1)
        runCurrent()

        assertEquals(2, repository.requests.size)
        assertEquals("Rick", repository.requests.last().filters.name)
    }

    @Test
    fun `does not repeat requests for equivalent filters`() = runTest(dispatcher) {
        val repository = FakeCharacterRepository { page -> successPage(page, listOf(page)) }
        val viewModel = CharacterListViewModel(repository)
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("Rick")
        advanceTimeBy(350)
        runCurrent()
        viewModel.onSearchQueryChanged(" Rick ")
        advanceTimeBy(350)
        runCurrent()

        assertEquals(2, repository.requests.size)
    }

    @Test
    fun `combines name status and free species filters`() = runTest(dispatcher) {
        val repository = FakeCharacterRepository { page -> successPage(page, listOf(page)) }
        val viewModel = CharacterListViewModel(repository)
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("Morty")
        viewModel.onStatusSelected(CharacterStatusFilter.Alive)
        runCurrent()
        viewModel.onSpeciesChanged("Robot")
        advanceTimeBy(350)
        runCurrent()

        assertEquals(
            CharacterFilters(
                name = "Morty",
                status = CharacterStatusFilter.Alive,
                species = "Robot",
            ),
            repository.requests.last().filters,
        )
    }

    @Test
    fun `filter changes reset pagination to the first page`() = runTest(dispatcher) {
        val repository = FakeCharacterRepository { page -> successPage(page, listOf(page)) }
        val viewModel = CharacterListViewModel(repository)
        advanceUntilIdle()
        viewModel.loadNextPage()
        advanceUntilIdle()

        viewModel.onStatusSelected(CharacterStatusFilter.Dead)
        advanceUntilIdle()

        assertEquals(listOf(1, 2, 1), repository.requestedPages)
        assertEquals(CharacterStatusFilter.Dead, repository.requests.last().filters.status)
    }

    @Test
    fun `cancels obsolete search and keeps only the latest response`() = runTest(dispatcher) {
        val obsoleteResult = CompletableDeferred<CharacterPageResult>()
        val repository = FakeCharacterRepository { page, filters ->
            when (filters.name) {
                "Rick" -> withContext(NonCancellable) { obsoleteResult.await() }
                "Morty" -> successPage(page, listOf(2), isLastPage = true)
                else -> successPage(page, listOf(0))
            }
        }
        val viewModel = CharacterListViewModel(repository)
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("Rick")
        advanceTimeBy(350)
        runCurrent()
        viewModel.onSearchQueryChanged("Morty")
        advanceTimeBy(350)
        runCurrent()
        obsoleteResult.complete(successPage(1, listOf(1), isLastPage = true))
        advanceUntilIdle()

        assertEquals(listOf(2), viewModel.uiState.value.characters.map { it.id })
        assertEquals("Morty", repository.requests.last().filters.name)
    }

    @Test
    fun `pagination keeps active filters`() = runTest(dispatcher) {
        val repository = FakeCharacterRepository { page ->
            successPage(page, listOf(page), isLastPage = page == 2)
        }
        val viewModel = CharacterListViewModel(repository)
        advanceUntilIdle()
        viewModel.onSpeciesChanged("Alien")
        advanceTimeBy(350)
        advanceUntilIdle()

        viewModel.loadNextPage()
        advanceUntilIdle()

        assertEquals(2, repository.requests.last().page)
        assertEquals("Alien", repository.requests.last().filters.species)
    }

    @Test
    fun `refresh keeps filters and replaces current content`() = runTest(dispatcher) {
        var filteredAttempts = 0
        val repository = FakeCharacterRepository { page, filters ->
            if (filters.name == "Rick") {
                filteredAttempts++
                successPage(page, listOf(filteredAttempts), isLastPage = true)
            } else {
                successPage(page, listOf(0))
            }
        }
        val viewModel = CharacterListViewModel(repository)
        advanceUntilIdle()
        viewModel.onSearchQueryChanged("Rick")
        advanceTimeBy(350)
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(listOf(2), viewModel.uiState.value.characters.map { it.id })
        assertEquals("Rick", repository.requests.last().filters.name)
        assertFalse(viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun `exposes an empty final result for a search without matches`() = runTest(dispatcher) {
        val repository = FakeCharacterRepository { page, filters ->
            if (filters.hasActiveFilters) successPage(page, emptyList(), isLastPage = true)
            else successPage(page, listOf(1))
        }
        val viewModel = CharacterListViewModel(repository)
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("Nobody")
        advanceTimeBy(350)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.characters.isEmpty())
        assertTrue(viewModel.uiState.value.isLastPage)
        assertTrue(viewModel.uiState.value.hasActiveFilters)
        assertNull(viewModel.uiState.value.initialError)
    }

    @Test
    fun `clear filters restores defaults and reloads the first page`() = runTest(dispatcher) {
        val repository = FakeCharacterRepository { page -> successPage(page, listOf(page)) }
        val viewModel = CharacterListViewModel(repository)
        advanceUntilIdle()
        viewModel.onSearchQueryChanged("Rick")
        viewModel.onStatusSelected(CharacterStatusFilter.Alive)
        advanceUntilIdle()

        viewModel.clearFilters()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("", state.searchQuery)
        assertEquals("", state.speciesQuery)
        assertEquals(CharacterStatusFilter.All, state.selectedStatus)
        assertEquals(CharacterFilters(), repository.requests.last().filters)
        assertEquals(1, repository.requests.last().page)
    }

    @Test
    fun `refresh failure keeps content and exposes a recoverable error`() = runTest(dispatcher) {
        var shouldFailRefresh = false
        val repository = FakeCharacterRepository { page ->
            if (shouldFailRefresh) failure(CharacterRepositoryError.Connection)
            else successPage(page, listOf(1, 2))
        }
        val viewModel = CharacterListViewModel(repository)
        advanceUntilIdle()
        shouldFailRefresh = true

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(listOf(1, 2), viewModel.uiState.value.characters.map { it.id })
        assertEquals(CharacterListUiError.Connection, viewModel.uiState.value.refreshError)
        assertFalse(viewModel.uiState.value.isRefreshing)
    }

    private class FakeCharacterRepository(
        private val response: suspend (page: Int, filters: CharacterFilters) -> CharacterPageResult,
    ) : CharacterRepository {
        constructor(response: suspend (page: Int) -> CharacterPageResult) : this(
            response = { page, _ -> response(page) },
        )

        val requests = mutableListOf<CharacterRequest>()
        val requestedPages: List<Int>
            get() = requests.map(CharacterRequest::page)

        override suspend fun getCharacters(
            page: Int,
            filters: CharacterFilters,
        ): CharacterPageResult {
            requests += CharacterRequest(page, filters)
            return response(page, filters)
        }
    }

    private data class CharacterRequest(
        val page: Int,
        val filters: CharacterFilters,
    )

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
