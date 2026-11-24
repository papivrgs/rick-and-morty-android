package com.example.rickandmorty.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CharacterLocationDto(
    val name: String,
    val url: String,
)
