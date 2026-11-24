package com.example.rickandmorty.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CharacterInfoDto(
    val count: Int,
    val pages: Int,
    val next: String? = null,
    @SerialName("prev")
    val previous: String? = null,
)
