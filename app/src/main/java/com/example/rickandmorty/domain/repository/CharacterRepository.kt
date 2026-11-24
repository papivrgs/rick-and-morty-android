package com.example.rickandmorty.domain.repository

interface CharacterRepository {

    suspend fun getCharacters(page: Int): CharacterPageResult
}
