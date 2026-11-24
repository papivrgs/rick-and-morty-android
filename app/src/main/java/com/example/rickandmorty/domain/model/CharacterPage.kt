package com.example.rickandmorty.domain.model

data class CharacterPage(
    val characters: List<Character>,
    val currentPage: Int,
    val totalCharacters: Int,
    val totalPages: Int,
    val nextPageUrl: String?,
    val previousPageUrl: String?,
    val isLastPage: Boolean,
)
