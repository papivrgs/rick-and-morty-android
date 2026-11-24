package com.example.rickandmorty.ui.characters

import com.example.rickandmorty.domain.model.Character
import com.example.rickandmorty.domain.model.CharacterFilters
import com.example.rickandmorty.domain.model.CharacterStatusFilter

data class CharacterListUiState(
    val characters: List<Character> = emptyList(),
    val searchQuery: String = "",
    val selectedStatus: CharacterStatusFilter = CharacterStatusFilter.All,
    val speciesQuery: String = "",
    val totalCharacters: Int = 0,
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isFilterPending: Boolean = false,
    val isLastPage: Boolean = false,
    val initialError: CharacterListUiError? = null,
    val refreshError: CharacterListUiError? = null,
    val paginationError: CharacterListUiError? = null,
) {
    val filters: CharacterFilters
        get() = CharacterFilters(
            name = searchQuery,
            status = selectedStatus,
            species = speciesQuery,
        )

    val hasActiveFilters: Boolean
        get() = filters.hasActiveFilters
}

sealed interface CharacterListUiError {
    data class Http(val statusCode: Int) : CharacterListUiError

    data object Connection : CharacterListUiError

    data object Serialization : CharacterListUiError
}
