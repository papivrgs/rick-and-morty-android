package com.example.rickandmorty.data.remote.dto

import com.example.rickandmorty.data.remote.serialization.rickAndMortyJson
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class CharacterResponseDtoTest {

    @Test
    fun `deserializes a valid character response`() {
        val response = rickAndMortyJson.decodeFromString<CharacterResponseDto>(VALID_RESPONSE_JSON)

        val character = response.results.single()
        assertEquals(1, character.id)
        assertEquals("Rick Sanchez", character.name)
        assertEquals("Alive", character.status)
        assertEquals("Human", character.species)
        assertEquals("", character.type)
        assertEquals("Male", character.gender)
        assertEquals("Earth (C-137)", character.origin.name)
        assertEquals("Earth (Replacement Dimension)", character.location.name)
        assertEquals("https://rickandmortyapi.com/api/character/avatar/1.jpeg", character.imageUrl)
        assertEquals(listOf("https://rickandmortyapi.com/api/episode/1"), character.episodeUrls)
        assertEquals("https://rickandmortyapi.com/api/character/1", character.resourceUrl)
        assertEquals("2017-11-04T18:48:46.250Z", character.createdAt)
    }

    @Test
    fun `ignores unknown fields and accepts missing optional pagination links`() {
        val json = """
            {
              "unknownResponseField": true,
              "info": {
                "count": 1,
                "pages": 1,
                "unknownInfoField": "ignored"
              },
              "results": [
                {
                  "id": 2,
                  "name": "Morty Smith",
                  "status": "Alive",
                  "species": "Human",
                  "type": "",
                  "gender": "Male",
                  "origin": {
                    "name": "unknown",
                    "url": "",
                    "dimension": "ignored"
                  },
                  "location": {"name": "Earth", "url": "https://example.com/location/1"},
                  "image": "https://example.com/morty.jpeg",
                  "episode": [],
                  "url": "https://example.com/character/2",
                  "created": "2017-11-04T18:50:21.651Z",
                  "unknownCharacterField": 42
                }
              ]
            }
        """.trimIndent()

        val response = rickAndMortyJson.decodeFromString<CharacterResponseDto>(json)

        assertEquals("Morty Smith", response.results.single().name)
        assertNull(response.info.next)
        assertNull(response.info.previous)
    }

    @Test
    fun `fails when a required character field is missing`() {
        val jsonWithoutName = VALID_RESPONSE_JSON.replace("\"name\": \"Rick Sanchez\",", "")

        assertThrows(SerializationException::class.java) {
            rickAndMortyJson.decodeFromString<CharacterResponseDto>(jsonWithoutName)
        }
    }

    @Test
    fun `represents pagination information`() {
        val response = rickAndMortyJson.decodeFromString<CharacterResponseDto>(VALID_RESPONSE_JSON)

        assertEquals(826, response.info.count)
        assertEquals(42, response.info.pages)
        assertEquals("https://rickandmortyapi.com/api/character?page=2", response.info.next)
        assertNull(response.info.previous)
    }

    @Test
    fun `fails when JSON is malformed`() {
        val malformedJson = """{"info":{"count":1},"results":[}"""

        assertThrows(SerializationException::class.java) {
            rickAndMortyJson.decodeFromString<CharacterResponseDto>(malformedJson)
        }
    }

    private companion object {
        val VALID_RESPONSE_JSON = """
            {
              "info": {
                "count": 826,
                "pages": 42,
                "next": "https://rickandmortyapi.com/api/character?page=2",
                "prev": null
              },
              "results": [
                {
                  "id": 1,
                  "name": "Rick Sanchez",
                  "status": "Alive",
                  "species": "Human",
                  "type": "",
                  "gender": "Male",
                  "origin": {
                    "name": "Earth (C-137)",
                    "url": "https://rickandmortyapi.com/api/location/1"
                  },
                  "location": {
                    "name": "Earth (Replacement Dimension)",
                    "url": "https://rickandmortyapi.com/api/location/20"
                  },
                  "image": "https://rickandmortyapi.com/api/character/avatar/1.jpeg",
                  "episode": ["https://rickandmortyapi.com/api/episode/1"],
                  "url": "https://rickandmortyapi.com/api/character/1",
                  "created": "2017-11-04T18:48:46.250Z"
                }
              ]
            }
        """.trimIndent()
    }
}
