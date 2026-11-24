package com.example.rickandmorty.app

import android.content.Context
import androidx.room.Room
import com.example.rickandmorty.data.local.database.RickAndMortyDatabase
import com.example.rickandmorty.data.local.session.DataStoreSessionStore
import com.example.rickandmorty.data.remote.RickAndMortyApi
import com.example.rickandmorty.data.remote.serialization.rickAndMortyJson
import com.example.rickandmorty.data.repository.AuthRepositoryImpl
import com.example.rickandmorty.data.repository.CharacterRepositoryImpl
import com.example.rickandmorty.data.security.Pbkdf2PasswordHasher
import com.example.rickandmorty.domain.repository.AuthRepository
import com.example.rickandmorty.domain.repository.CharacterRepository
import kotlinx.serialization.ExperimentalSerializationApi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class AppContainer(
    context: Context? = null,
    baseUrl: String = RICK_AND_MORTY_BASE_URL,
    okHttpClient: OkHttpClient = OkHttpClient(),
) {
    private val applicationContext = context?.applicationContext

    val authRepository: AuthRepository by lazy {
        val requiredContext = requireNotNull(applicationContext) {
            "A Context is required to create local authentication dependencies."
        }
        val database = Room.databaseBuilder(
            context = requiredContext,
            klass = RickAndMortyDatabase::class.java,
            name = DATABASE_NAME,
        ).build()
        AuthRepositoryImpl(
            userDao = database.userDao(),
            sessionStore = DataStoreSessionStore(requiredContext),
            passwordHasher = Pbkdf2PasswordHasher(),
        )
    }
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
        const val DATABASE_NAME = "rick_and_morty.db"
    }
}
