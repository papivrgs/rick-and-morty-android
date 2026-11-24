package com.example.rickandmorty.domain.repository

import com.example.rickandmorty.domain.model.CharacterPage

sealed interface CharacterPageResult {
    data class Success(val page: CharacterPage) : CharacterPageResult

    data class Failure(val error: CharacterRepositoryError) : CharacterPageResult
}

sealed interface CharacterRepositoryError {
    data class Http(val statusCode: Int) : CharacterRepositoryError

    data object Connection : CharacterRepositoryError

    data object Serialization : CharacterRepositoryError
}
