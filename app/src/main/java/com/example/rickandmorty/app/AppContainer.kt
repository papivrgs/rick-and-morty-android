package com.example.rickandmorty.app

import com.example.rickandmorty.data.remote.RickAndMortyApi
import com.example.rickandmorty.data.remote.serialization.rickAndMortyJson
import com.example.rickandmorty.data.repository.CharacterRepositoryImpl
import com.example.rickandmorty.domain.repository.CharacterRepository
import kotlinx.serialization.ExperimentalSerializationApi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class AppContainer(
    baseUrl: String = RICK_AND_MORTY_BASE_URL,
    okHttpClient: OkHttpClient = OkHttpClient(),
) {
    val characterRepository: CharacterRepository

    init {
        val api = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(jsonConverterFactory())
            .build()
            .create(RickAndMortyApi::class.java)

        characterRepository = CharacterRepositoryImpl(api)
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun jsonConverterFactory() = rickAndMortyJson.asConverterFactory(
        "application/json".toMediaType(),
    )

    private companion object {
        const val RICK_AND_MORTY_BASE_URL = "https://rickandmortyapi.com/api/"
    }
}
