package com.example.rickandmorty.data.remote.mapper

import com.example.rickandmorty.data.remote.dto.EpisodeDto
import org.junit.Assert.assertEquals
import org.junit.Test

class EpisodeMapperTest {

    @Test
    fun `maps episode dto to domain`() {
        val episode = EpisodeDto(
            id = 28,
            name = "The Ricklantis Mixup",
            code = "S03E07",
        ).toDomain()

        assertEquals(28, episode.id)
        assertEquals("The Ricklantis Mixup", episode.name)
        assertEquals("S03E07", episode.code)
    }
}
