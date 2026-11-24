package com.example.rickandmorty.domain.repository

import com.example.rickandmorty.domain.model.Character
import com.example.rickandmorty.domain.model.Episode

sealed interface CharacterResult {
    data class Success(val character: Character) : CharacterResult

    data class Failure(val error: CharacterRepositoryError) : CharacterResult
}

sealed interface EpisodeResult {
    data class Success(val episodes: List<Episode>) : EpisodeResult

    data class Failure(val error: CharacterRepositoryError) : EpisodeResult
}
