package com.example.rickandmorty.data.repository

import com.example.rickandmorty.data.remote.RickAndMortyApi
import com.example.rickandmorty.data.remote.mapper.toDomain
import com.example.rickandmorty.domain.model.CharacterFilters
import com.example.rickandmorty.domain.model.CharacterPage
import com.example.rickandmorty.domain.model.CharacterStatusFilter
import com.example.rickandmorty.domain.repository.CharacterPageResult
import com.example.rickandmorty.domain.repository.CharacterRepository
import com.example.rickandmorty.domain.repository.CharacterRepositoryError
import com.example.rickandmorty.domain.repository.CharacterResult
import com.example.rickandmorty.domain.repository.EpisodeResult
import java.io.IOException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

class CharacterRepositoryImpl(
    private val api: RickAndMortyApi,
) : CharacterRepository {

    override suspend fun getCharacters(
        page: Int,
        filters: CharacterFilters,
    ): CharacterPageResult = try {
        val normalizedFilters = filters.normalized()
        val response = api.getCharacters(
            page = page,
            name = normalizedFilters.name.ifBlank { null },
            status = normalizedFilters.status.toApiValue(),
            species = normalizedFilters.species.ifBlank { null },
        )
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
        if (error.code() == HTTP_NOT_FOUND && filters.hasActiveFilters) {
            CharacterPageResult.Success(
                CharacterPage(
                    characters = emptyList(),
                    currentPage = page,
                    totalCharacters = 0,
                    totalPages = 0,
                    nextPageUrl = null,
                    previousPageUrl = null,
                    isLastPage = true,
                ),
            )
        } else {
            CharacterPageResult.Failure(
                CharacterRepositoryError.Http(statusCode = error.code()),
            )
        }
    } catch (error: IOException) {
        CharacterPageResult.Failure(CharacterRepositoryError.Connection)
    } catch (error: SerializationException) {
        CharacterPageResult.Failure(CharacterRepositoryError.Serialization)
    }

    override suspend fun getCharacter(id: Int): CharacterResult = try {
        CharacterResult.Success(api.getCharacter(id).toDomain())
    } catch (error: HttpException) {
        CharacterResult.Failure(CharacterRepositoryError.Http(error.code()))
    } catch (error: IOException) {
        CharacterResult.Failure(CharacterRepositoryError.Connection)
    } catch (error: SerializationException) {
        CharacterResult.Failure(CharacterRepositoryError.Serialization)
    }

    override suspend fun getEpisodes(ids: List<Int>): EpisodeResult {
        val orderedIds = ids.distinct()
        if (orderedIds.isEmpty()) return EpisodeResult.Success(emptyList())

        return try {
            val episodes = if (orderedIds.size == 1) {
                listOf(api.getEpisode(orderedIds.single()))
            } else {
                api.getEpisodes(orderedIds.joinToString(","))
            }
            val episodesById = episodes.associateBy { it.id }
            EpisodeResult.Success(
                episodes = orderedIds.mapNotNull { id -> episodesById[id]?.toDomain() },
            )
        } catch (error: HttpException) {
            EpisodeResult.Failure(CharacterRepositoryError.Http(error.code()))
        } catch (error: IOException) {
            EpisodeResult.Failure(CharacterRepositoryError.Connection)
        } catch (error: SerializationException) {
            EpisodeResult.Failure(CharacterRepositoryError.Serialization)
        }
    }

    private fun CharacterStatusFilter.toApiValue(): String? = when (this) {
        CharacterStatusFilter.All -> null
        CharacterStatusFilter.Alive -> "alive"
        CharacterStatusFilter.Dead -> "dead"
        CharacterStatusFilter.Unknown -> "unknown"
    }

    private companion object {
        const val HTTP_NOT_FOUND = 404
    }
}
