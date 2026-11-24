package com.example.rickandmorty.ui.characters

import com.example.rickandmorty.domain.model.Character

data class CharacterListUiState(
    val characters: List<Character> = emptyList(),
    val isInitialLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val isLastPage: Boolean = false,
    val initialError: CharacterListUiError? = null,
    val paginationError: CharacterListUiError? = null,
)

sealed interface CharacterListUiError {
    data class Http(val statusCode: Int) : CharacterListUiError

    data object Connection : CharacterListUiError

    data object Serialization : CharacterListUiError
}
