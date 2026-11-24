package com.example.rickandmorty.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CharacterDto(
    val id: Int,
    val name: String,
    val status: String,
    val species: String,
    val type: String,
    val gender: String,
    val origin: CharacterLocationDto,
    val location: CharacterLocationDto,
    @SerialName("image")
    val imageUrl: String,
    @SerialName("episode")
    val episodeUrls: List<String>,
    @SerialName("url")
    val resourceUrl: String,
    @SerialName("created")
    val createdAt: String,
)
