package com.example.rickandmorty.domain.model

data class Character(
    val id: Int,
    val name: String,
    val status: String,
    val species: String,
    val type: String,
    val gender: String,
    val originName: String,
    val originUrl: String,
    val locationName: String,
    val locationUrl: String,
    val imageUrl: String,
    val episodeUrls: List<String>,
    val resourceUrl: String,
    val createdAt: String,
)
