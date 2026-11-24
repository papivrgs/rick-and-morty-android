package com.example.rickandmorty.ui.characters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rickandmorty.domain.repository.CharacterPageResult
import com.example.rickandmorty.domain.repository.CharacterRepository
import com.example.rickandmorty.domain.repository.CharacterRepositoryError
import kotlinx.coroutines.CancellationException
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

    private var nextPage = FIRST_PAGE

    init {
        loadInitialPage()
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (
            state.isInitialLoading ||
            state.isLoadingMore ||
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
        loadPage(page = nextPage, isInitialLoad = false)
    }

    fun retryInitialLoad() {
        val state = _uiState.value
        if (state.isInitialLoading || state.isLoadingMore || state.initialError == null) {
            return
        }
        loadInitialPage()
    }

    fun retryPagination() {
        val state = _uiState.value
        if (state.paginationError == null) {
            return
        }
        loadNextPage()
    }

    private fun loadInitialPage() {
        val state = _uiState.value
        if (state.isLoadingMore || (state.isInitialLoading && state.initialError == null && nextPage != FIRST_PAGE)) {
            return
        }

        nextPage = FIRST_PAGE
        _uiState.update {
            it.copy(
                isInitialLoading = true,
                isLastPage = false,
                initialError = null,
                paginationError = null,
            )
        }
        loadPage(page = FIRST_PAGE, isInitialLoad = true)
    }

    private fun loadPage(page: Int, isInitialLoad: Boolean) {
        viewModelScope.launch {
            try {
                when (val result = repository.getCharacters(page)) {
                    is CharacterPageResult.Success -> handleSuccess(
                        result = result,
                        isInitialLoad = isInitialLoad,
                    )

                    is CharacterPageResult.Failure -> handleFailure(
                        error = result.error,
                        isInitialLoad = isInitialLoad,
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } finally {
                _uiState.update {
                    if (isInitialLoad) {
                        it.copy(isInitialLoading = false)
                    } else {
                        it.copy(isLoadingMore = false)
                    }
                }
            }
        }
    }

    private fun handleSuccess(
        result: CharacterPageResult.Success,
        isInitialLoad: Boolean,
    ) {
        val page = result.page
        nextPage = page.currentPage + 1
        _uiState.update { currentState ->
            val characters = if (isInitialLoad) {
                page.characters
            } else {
                (currentState.characters + page.characters).distinctBy { it.id }
            }
            currentState.copy(
                characters = characters,
                isLastPage = page.isLastPage,
                initialError = null,
                paginationError = null,
            )
        }
    }

    private fun handleFailure(
        error: CharacterRepositoryError,
        isInitialLoad: Boolean,
    ) {
        val uiError = error.toUiError()
        _uiState.update {
            if (isInitialLoad) {
                it.copy(initialError = uiError)
            } else {
                it.copy(paginationError = uiError)
            }
        }
    }

    private fun CharacterRepositoryError.toUiError(): CharacterListUiError = when (this) {
        is CharacterRepositoryError.Http -> CharacterListUiError.Http(statusCode)
        CharacterRepositoryError.Connection -> CharacterListUiError.Connection
        CharacterRepositoryError.Serialization -> CharacterListUiError.Serialization
    }

    private companion object {
        const val FIRST_PAGE = 1
    }
}
