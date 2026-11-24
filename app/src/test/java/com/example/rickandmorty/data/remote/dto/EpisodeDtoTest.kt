package com.example.rickandmorty.data.remote.dto

import com.example.rickandmorty.data.remote.serialization.rickAndMortyJson
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Test

class EpisodeDtoTest {

    @Test
    fun `deserializes episode fields and ignores unknown values`() {
        val dto = rickAndMortyJson.decodeFromString<EpisodeDto>(
            """
            {
              "id": 28,
              "name": "The Ricklantis Mixup",
              "episode": "S03E07",
              "air_date": "September 10, 2017",
              "url": "https://rickandmortyapi.com/api/episode/28"
            }
            """.trimIndent(),
        )

        assertEquals(28, dto.id)
        assertEquals("The Ricklantis Mixup", dto.name)
        assertEquals("S03E07", dto.code)
    }

    @Test(expected = SerializationException::class)
    fun `rejects an episode without mandatory code`() {
        rickAndMortyJson.decodeFromString<EpisodeDto>(
            """{"id":28,"name":"The Ricklantis Mixup"}""",
        )
    }
}
