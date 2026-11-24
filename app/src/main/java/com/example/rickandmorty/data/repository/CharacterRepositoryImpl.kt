package com.example.rickandmorty.data.repository

import com.example.rickandmorty.data.remote.RickAndMortyApi
import com.example.rickandmorty.data.remote.mapper.toDomain
import com.example.rickandmorty.domain.model.CharacterPage
import com.example.rickandmorty.domain.repository.CharacterPageResult
import com.example.rickandmorty.domain.repository.CharacterRepository
import com.example.rickandmorty.domain.repository.CharacterRepositoryError
import java.io.IOException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

class CharacterRepositoryImpl(
    private val api: RickAndMortyApi,
) : CharacterRepository {

    override suspend fun getCharacters(page: Int): CharacterPageResult = try {
        val response = api.getCharacters(page)
        CharacterPageResult.Success(
            page = CharacterPage(
                characters = response.results.map { it.toDomain() },
                currentPage = page,
                totalCharacters = response.info.count,
                totalPages = response.info.pages,
                nextPageUrl = response.info.next,
                previousPageUrl = response.info.previous,
                isLastPage = response.info.next == null,
            ),
        )
    } catch (error: HttpException) {
        CharacterPageResult.Failure(
            CharacterRepositoryError.Http(statusCode = error.code()),
        )
    } catch (error: IOException) {
        CharacterPageResult.Failure(CharacterRepositoryError.Connection)
    } catch (error: SerializationException) {
        CharacterPageResult.Failure(CharacterRepositoryError.Serialization)
    }
}
