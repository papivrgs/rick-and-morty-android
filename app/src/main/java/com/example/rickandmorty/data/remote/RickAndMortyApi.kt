package com.example.rickandmorty.data.remote

import com.example.rickandmorty.data.remote.dto.CharacterResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface RickAndMortyApi {

    @GET("character")
    suspend fun getCharacters(
        @Query("page") page: Int,
        @Query("name") name: String?,
        @Query("status") status: String?,
        @Query("species") species: String?,
    ): CharacterResponseDto
}
