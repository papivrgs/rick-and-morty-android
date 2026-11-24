package com.example.rickandmorty.ui.characters.detail

import com.example.rickandmorty.domain.model.Character
import com.example.rickandmorty.domain.model.Episode
import com.example.rickandmorty.ui.characters.CharacterListUiError

data class CharacterDetailUiState(
    val character: Character? = null,
    val episodes: List<Episode> = emptyList(),
    val isCharacterLoading: Boolean = true,
    val isEpisodesLoading: Boolean = false,
    val characterError: CharacterListUiError? = null,
    val episodeError: CharacterListUiError? = null,
)
