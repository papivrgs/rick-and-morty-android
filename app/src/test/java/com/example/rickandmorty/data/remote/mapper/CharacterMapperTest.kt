package com.example.rickandmorty.data.remote.mapper

import com.example.rickandmorty.data.remote.dto.CharacterDto
import com.example.rickandmorty.data.remote.dto.CharacterLocationDto
import com.example.rickandmorty.domain.model.Character
import org.junit.Assert.assertEquals
import org.junit.Test

class CharacterMapperTest {

    @Test
    fun `maps character DTO to domain model`() {
        val dto = CharacterDto(
            id = 361,
            name = "Toxic Rick",
            status = "Dead",
            species = "Humanoid",
            type = "Rick's Toxic Side",
            gender = "Male",
            origin = CharacterLocationDto(
                name = "Alien Spa",
                url = "https://rickandmortyapi.com/api/location/64",
            ),
            location = CharacterLocationDto(
                name = "Earth",
                url = "https://rickandmortyapi.com/api/location/20",
            ),
            imageUrl = "https://rickandmortyapi.com/api/character/avatar/361.jpeg",
            episodeUrls = listOf("https://rickandmortyapi.com/api/episode/27"),
            resourceUrl = "https://rickandmortyapi.com/api/character/361",
            createdAt = "2018-01-10T18:20:41.703Z",
        )

        val character = dto.toDomain()

        assertEquals(
            Character(
                id = 361,
                name = "Toxic Rick",
                status = "Dead",
                species = "Humanoid",
                type = "Rick's Toxic Side",
                gender = "Male",
                originName = "Alien Spa",
                originUrl = "https://rickandmortyapi.com/api/location/64",
                locationName = "Earth",
                locationUrl = "https://rickandmortyapi.com/api/location/20",
                imageUrl = "https://rickandmortyapi.com/api/character/avatar/361.jpeg",
                episodeUrls = listOf("https://rickandmortyapi.com/api/episode/27"),
                resourceUrl = "https://rickandmortyapi.com/api/character/361",
                createdAt = "2018-01-10T18:20:41.703Z",
            ),
            character,
        )
    }
}
