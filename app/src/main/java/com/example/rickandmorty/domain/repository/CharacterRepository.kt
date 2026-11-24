package com.example.rickandmorty.domain.repository

import com.example.rickandmorty.domain.model.CharacterFilters

interface CharacterRepository {

    suspend fun getCharacters(
        page: Int,
        filters: CharacterFilters = CharacterFilters(),
    ): CharacterPageResult

    suspend fun getCharacter(id: Int): CharacterResult

    suspend fun getEpisodes(ids: List<Int>): EpisodeResult
}
