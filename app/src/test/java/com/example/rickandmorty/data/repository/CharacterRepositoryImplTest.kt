package com.example.rickandmorty.data.repository

import com.example.rickandmorty.app.AppContainer
import com.example.rickandmorty.domain.model.CharacterFilters
import com.example.rickandmorty.domain.model.CharacterStatusFilter
import com.example.rickandmorty.domain.repository.CharacterPageResult
import com.example.rickandmorty.domain.repository.CharacterRepository
import com.example.rickandmorty.domain.repository.CharacterRepositoryError
import com.example.rickandmorty.domain.repository.CharacterResult
import com.example.rickandmorty.domain.repository.EpisodeResult
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CharacterRepositoryImplTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: CharacterRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        repository = AppContainer(
            baseUrl = server.url("/").toString(),
        ).characterRepository
    }

    @After
    fun tearDown() {
        runCatching { server.close() }
    }

    @Test
    fun `returns a successful page for a valid HTTP 200 response`() = runTest {
        server.enqueueJson(VALID_RESPONSE_JSON)

        val result = repository.getCharacters(page = 1)

        assertTrue(result is CharacterPageResult.Success)
    }

    @Test
    fun `converts response characters to domain models`() = runTest {
        server.enqueueJson(VALID_RESPONSE_JSON)

        val page = repository.getCharacters(page = 1).requireSuccessPage()
        val character = page.characters.single()

        assertEquals(1, character.id)
        assertEquals("Rick Sanchez", character.name)
        assertEquals("Earth (C-137)", character.originName)
        assertEquals("Earth (Replacement Dimension)", character.locationName)
        assertEquals("https://rickandmortyapi.com/api/character/avatar/1.jpeg", character.imageUrl)
    }

    @Test
    fun `requests the specified page`() = runTest {
        server.enqueueJson(VALID_RESPONSE_JSON)

        repository.getCharacters(page = 3)

        val request = server.takeRequest()
        assertEquals("/character", request.url.encodedPath)
        assertEquals("3", request.url.queryParameter("page"))
    }

    @Test
    fun `sends combined filters to the API`() = runTest {
        server.enqueueJson(VALID_RESPONSE_JSON)

        repository.getCharacters(
            page = 2,
            filters = CharacterFilters(
                name = "Rick Sanchez",
                status = CharacterStatusFilter.Alive,
                species = "Human",
            ),
        )

        val request = server.takeRequest()
        assertEquals("2", request.url.queryParameter("page"))
        assertEquals("Rick Sanchez", request.url.queryParameter("name"))
        assertEquals("alive", request.url.queryParameter("status"))
        assertEquals("Human", request.url.queryParameter("species"))
    }

    @Test
    fun `omits blank filters from the API request`() = runTest {
        server.enqueueJson(VALID_RESPONSE_JSON)

        repository.getCharacters(
            page = 1,
            filters = CharacterFilters(name = "  ", species = " "),
        )

        val request = server.takeRequest()
        assertEquals(null, request.url.queryParameter("name"))
        assertEquals(null, request.url.queryParameter("status"))
        assertEquals(null, request.url.queryParameter("species"))
    }

    @Test
    fun `maps HTTP 404 to an HTTP repository error`() = runTest {
        server.enqueueResponse(code = 404, body = "Not found")

        val result = repository.getCharacters(page = 1)

        assertEquals(
            CharacterPageResult.Failure(CharacterRepositoryError.Http(404)),
            result,
        )
    }

    @Test
    fun `maps a filtered HTTP 404 to an empty final page`() = runTest {
        server.enqueueResponse(code = 404, body = "Not found")

        val page = repository.getCharacters(
            page = 1,
            filters = CharacterFilters(name = "Nobody"),
        ).requireSuccessPage()

        assertTrue(page.characters.isEmpty())
        assertEquals(0, page.totalCharacters)
        assertEquals(0, page.totalPages)
        assertTrue(page.isLastPage)
    }

    @Test
    fun `maps HTTP 500 to an HTTP repository error`() = runTest {
        server.enqueueResponse(code = 500, body = "Server error")

        val result = repository.getCharacters(page = 1)

        assertEquals(
            CharacterPageResult.Failure(CharacterRepositoryError.Http(500)),
            result,
        )
    }

    @Test
    fun `maps invalid JSON to a serialization error`() = runTest {
        server.enqueueJson("{invalid-json")

        val result = repository.getCharacters(page = 1)

        assertEquals(
            CharacterPageResult.Failure(CharacterRepositoryError.Serialization),
            result,
        )
    }

    @Test
    fun `maps connection failures to a connection error`() = runTest {
        server.close()

        val result = repository.getCharacters(page = 1)

        assertEquals(
            CharacterPageResult.Failure(CharacterRepositoryError.Connection),
            result,
        )
    }

    @Test
    fun `maps pagination information and detects a non-final page`() = runTest {
        server.enqueueJson(VALID_RESPONSE_JSON)

        val page = repository.getCharacters(page = 1).requireSuccessPage()

        assertEquals(826, page.totalCharacters)
        assertEquals(42, page.totalPages)
        assertEquals(1, page.currentPage)
        assertEquals("https://rickandmortyapi.com/api/character?page=2", page.nextPageUrl)
        assertEquals(null, page.previousPageUrl)
        assertFalse(page.isLastPage)
    }

    @Test
    fun `supports an empty final page`() = runTest {
        server.enqueueJson(EMPTY_RESPONSE_JSON)

        val page = repository.getCharacters(page = 1).requireSuccessPage()

        assertTrue(page.characters.isEmpty())
        assertEquals(0, page.totalCharacters)
        assertEquals(0, page.totalPages)
        assertTrue(page.isLastPage)
    }

    @Test
    fun `loads a character detail by id`() = runTest {
        server.enqueueJson(CHARACTER_DETAIL_JSON)

        val result = repository.getCharacter(id = 2)

        val character = (result as CharacterResult.Success).character
        assertEquals(2, character.id)
        assertEquals("Morty Smith", character.name)
        assertEquals("Citadel of Ricks", character.locationName)
        assertEquals("/character/2", server.takeRequest().url.encodedPath)
    }

    @Test
    fun `uses one multiple endpoint request and preserves requested episode order`() = runTest {
        server.enqueueJson(EPISODES_JSON)

        val result = repository.getEpisodes(listOf(3, 1, 2, 1))

        val episodes = (result as EpisodeResult.Success).episodes
        assertEquals(listOf(3, 1, 2), episodes.map { it.id })
        assertEquals(listOf("S01E03", "S01E01", "S01E02"), episodes.map { it.code })
        assertEquals("/episode/3,1,2", server.takeRequest().url.encodedPath)
    }

    @Test
    fun `uses the single episode endpoint for one id`() = runTest {
        server.enqueueJson(EPISODE_ONE_JSON)

        val result = repository.getEpisodes(listOf(1))

        assertEquals(listOf(1), (result as EpisodeResult.Success).episodes.map { it.id })
        assertEquals("/episode/1", server.takeRequest().url.encodedPath)
    }

    @Test
    fun `returns an empty episode list without a request`() = runTest {
        val result = repository.getEpisodes(emptyList())

        assertTrue((result as EpisodeResult.Success).episodes.isEmpty())
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `maps an episode HTTP failure without affecting character contract`() = runTest {
        server.enqueueResponse(code = 500, body = "Server error")

        val result = repository.getEpisodes(listOf(1, 2))

        assertEquals(
            EpisodeResult.Failure(CharacterRepositoryError.Http(500)),
            result,
        )
    }

    @Test
    fun `maps invalid episode JSON to serialization error`() = runTest {
        server.enqueueJson("{invalid-json")

        val result = repository.getEpisodes(listOf(1))

        assertEquals(
            EpisodeResult.Failure(CharacterRepositoryError.Serialization),
            result,
        )
    }

    private fun MockWebServer.enqueueJson(body: String) {
        enqueueResponse(code = 200, body = body)
    }

    private fun MockWebServer.enqueueResponse(code: Int, body: String) {
        enqueue(
            MockResponse.Builder()
                .code(code)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .body(body)
                .build(),
        )
    }

    private fun CharacterPageResult.requireSuccessPage() =
        (this as CharacterPageResult.Success).page

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

        val EMPTY_RESPONSE_JSON = """
            {
              "info": {
                "count": 0,
                "pages": 0,
                "next": null,
                "prev": null
              },
              "results": []
            }
        """.trimIndent()

        val CHARACTER_DETAIL_JSON = """
            {
              "id": 2,
              "name": "Morty Smith",
              "status": "Alive",
              "species": "Human",
              "type": "",
              "gender": "Male",
              "origin": {"name": "unknown", "url": ""},
              "location": {
                "name": "Citadel of Ricks",
                "url": "https://rickandmortyapi.com/api/location/3"
              },
              "image": "https://rickandmortyapi.com/api/character/avatar/2.jpeg",
              "episode": [
                "https://rickandmortyapi.com/api/episode/1",
                "https://rickandmortyapi.com/api/episode/2"
              ],
              "url": "https://rickandmortyapi.com/api/character/2",
              "created": "2017-11-04T18:50:21.651Z"
            }
        """.trimIndent()

        val EPISODES_JSON = """
            [
              {"id":1,"name":"Pilot","episode":"S01E01"},
              {"id":2,"name":"Lawnmower Dog","episode":"S01E02"},
              {"id":3,"name":"Anatomy Park","episode":"S01E03"}
            ]
        """.trimIndent()

        val EPISODE_ONE_JSON = """
            {"id":1,"name":"Pilot","episode":"S01E01"}
        """.trimIndent()
    }
}
