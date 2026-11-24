package com.example.rickandmorty.ui.characters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rickandmorty.domain.model.CharacterFilters
import com.example.rickandmorty.domain.model.CharacterStatusFilter
import com.example.rickandmorty.domain.repository.CharacterPageResult
import com.example.rickandmorty.domain.repository.CharacterRepository
import com.example.rickandmorty.domain.repository.CharacterRepositoryError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CharacterListViewModel(
    private val repository: CharacterRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CharacterListUiState())
    val uiState: StateFlow<CharacterListUiState> = _uiState.asStateFlow()

    private var appliedFilters = CharacterFilters()
    private var nextPage = FIRST_PAGE
    private var requestVersion = 0L
    private var loadJob: Job? = null
    private var filterDebounceJob: Job? = null

    init {
        startPageLoad(page = FIRST_PAGE, loadType = LoadType.Initial)
    }

    fun onSearchQueryChanged(query: String) {
        if (query == _uiState.value.searchQuery) return
        _uiState.update { it.copy(searchQuery = query) }
        scheduleFilterApplication()
    }

    fun onStatusSelected(status: CharacterStatusFilter) {
        if (status == _uiState.value.selectedStatus) return
        _uiState.update { it.copy(selectedStatus = status) }
        filterDebounceJob?.cancel()
        applyCurrentFilters()
    }

    fun onSpeciesChanged(species: String) {
        if (species == _uiState.value.speciesQuery) return
        _uiState.update { it.copy(speciesQuery = species) }
        scheduleFilterApplication()
    }

    fun clearFilters() {
        filterDebounceJob?.cancel()
        _uiState.update {
            it.copy(
                searchQuery = "",
                selectedStatus = CharacterStatusFilter.All,
                speciesQuery = "",
                isFilterPending = false,
            )
        }
        applyFilters(CharacterFilters())
    }

    fun refresh() {
        val currentFilters = _uiState.value.filters.normalized()
        filterDebounceJob?.cancel()
        if (currentFilters != appliedFilters) {
            applyFilters(currentFilters)
            return
        }

        val state = _uiState.value
        if (state.isInitialLoading || state.isRefreshing || state.isLoadingMore) return

        requestVersion++
        loadJob?.cancel()
        nextPage = FIRST_PAGE
        _uiState.update {
            it.copy(
                isRefreshing = true,
                isFilterPending = false,
                refreshError = null,
                paginationError = null,
            )
        }
        startPageLoad(
            page = FIRST_PAGE,
            loadType = LoadType.Refresh,
            version = requestVersion,
        )
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (
            state.isInitialLoading ||
            state.isRefreshing ||
            state.isLoadingMore ||
            state.isFilterPending ||
            state.isLastPage ||
            state.characters.isEmpty()
        ) {
            return
        }

        _uiState.update {
            it.copy(
                isLoadingMore = true,
                paginationError = null,
            )
        }
        startPageLoad(
            page = nextPage,
            loadType = LoadType.Pagination,
            version = requestVersion,
        )
    }

    fun retryInitialLoad() {
        val state = _uiState.value
        if (state.isInitialLoading || state.isRefreshing || state.initialError == null) return

        _uiState.update {
            it.copy(
                isInitialLoading = true,
                initialError = null,
            )
        }
        startPageLoad(
            page = FIRST_PAGE,
            loadType = LoadType.Initial,
            version = requestVersion,
        )
    }

    fun retryPagination() {
        val state = _uiState.value
        if (state.paginationError == null || state.isLoadingMore) return
        loadNextPage()
    }

    private fun scheduleFilterApplication() {
        filterDebounceJob?.cancel()
        _uiState.update { it.copy(isFilterPending = true) }
        filterDebounceJob = viewModelScope.launch {
            delay(FILTER_DEBOUNCE_MILLIS)
            applyCurrentFilters()
        }
    }

    private fun applyCurrentFilters() {
        applyFilters(_uiState.value.filters.normalized())
    }

    private fun applyFilters(filters: CharacterFilters) {
        if (filters == appliedFilters) {
            _uiState.update { it.copy(isFilterPending = false) }
            return
        }

        appliedFilters = filters
        requestVersion++
        nextPage = FIRST_PAGE
        loadJob?.cancel()
        _uiState.update {
            it.copy(
                characters = emptyList(),
                totalCharacters = 0,
                isInitialLoading = true,
                isRefreshing = false,
                isLoadingMore = false,
                isFilterPending = false,
                isLastPage = false,
                initialError = null,
                refreshError = null,
                paginationError = null,
            )
        }
        startPageLoad(
            page = FIRST_PAGE,
            loadType = LoadType.Initial,
            version = requestVersion,
        )
    }

    private fun startPageLoad(
        page: Int,
        loadType: LoadType,
        version: Long = requestVersion,
    ) {
        loadJob = viewModelScope.launch {
            try {
                val result = repository.getCharacters(
                    page = page,
                    filters = appliedFilters,
                )
                if (version != requestVersion) return@launch

                when (result) {
                    is CharacterPageResult.Success -> handleSuccess(result, loadType)
                    is CharacterPageResult.Failure -> handleFailure(result.error, loadType)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } finally {
                if (version == requestVersion) {
                    _uiState.update {
                        when (loadType) {
                            LoadType.Initial -> it.copy(isInitialLoading = false)
                            LoadType.Refresh -> it.copy(isRefreshing = false)
                            LoadType.Pagination -> it.copy(isLoadingMore = false)
                        }
                    }
                }
            }
        }
    }

    private fun handleSuccess(
        result: CharacterPageResult.Success,
        loadType: LoadType,
    ) {
        val page = result.page
        nextPage = page.currentPage + 1
        _uiState.update { currentState ->
            val characters = if (loadType == LoadType.Pagination) {
                (currentState.characters + page.characters).distinctBy { it.id }
            } else {
                page.characters
            }
            currentState.copy(
                characters = characters,
                totalCharacters = page.totalCharacters,
                isLastPage = page.isLastPage,
                initialError = null,
                refreshError = null,
                paginationError = null,
            )
        }
    }

    private fun handleFailure(
        error: CharacterRepositoryError,
        loadType: LoadType,
    ) {
        val uiError = error.toUiError()
        _uiState.update {
            when (loadType) {
                LoadType.Initial -> it.copy(initialError = uiError)
                LoadType.Refresh -> it.copy(refreshError = uiError)
                LoadType.Pagination -> it.copy(paginationError = uiError)
            }
        }
    }

    private fun CharacterRepositoryError.toUiError(): CharacterListUiError = when (this) {
        is CharacterRepositoryError.Http -> CharacterListUiError.Http(statusCode)
        CharacterRepositoryError.Connection -> CharacterListUiError.Connection
        CharacterRepositoryError.Serialization -> CharacterListUiError.Serialization
    }

    private enum class LoadType {
        Initial,
        Refresh,
        Pagination,
    }

    private companion object {
        const val FIRST_PAGE = 1
        const val FILTER_DEBOUNCE_MILLIS = 350L
    }
}
