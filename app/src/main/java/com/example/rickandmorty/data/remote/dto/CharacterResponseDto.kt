package com.example.rickandmorty.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CharacterResponseDto(
    val info: CharacterInfoDto,
    val results: List<CharacterDto>,
)
