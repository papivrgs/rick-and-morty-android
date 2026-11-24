package com.example.rickandmorty.data.remote.serialization

import kotlinx.serialization.json.Json

val rickAndMortyJson = Json {
    ignoreUnknownKeys = true
}
