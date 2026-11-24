package com.example.rickandmorty.ui.characters.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rickandmorty.domain.model.Character
import com.example.rickandmorty.domain.repository.CharacterRepository
import com.example.rickandmorty.domain.repository.CharacterRepositoryError
import com.example.rickandmorty.domain.repository.CharacterResult
import com.example.rickandmorty.domain.repository.EpisodeResult
import com.example.rickandmorty.ui.characters.CharacterListUiError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CharacterDetailViewModel(
    private val characterId: Int,
    private val repository: CharacterRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CharacterDetailUiState())
    val uiState: StateFlow<CharacterDetailUiState> = _uiState.asStateFlow()

    private var characterJob: Job? = null
    private var episodesJob: Job? = null

    init {
        loadCharacter()
    }

    fun retryCharacter() {
        if (_uiState.value.isCharacterLoading) return
        loadCharacter()
    }

    fun retryEpisodes() {
        val character = _uiState.value.character ?: return
        if (_uiState.value.isEpisodesLoading) return
        loadEpisodes(character)
    }

    private fun loadCharacter() {
        if (characterJob?.isActive == true) return
        episodesJob?.cancel()
        _uiState.value = CharacterDetailUiState(isCharacterLoading = true)
        characterJob = viewModelScope.launch {
            try {
                when (val result = repository.getCharacter(characterId)) {
                    is CharacterResult.Success -> {
                        _uiState.update {
                            it.copy(
                                character = result.character,
                                isCharacterLoading = false,
                                characterError = null,
                            )
                        }
                        loadEpisodes(result.character)
                    }

                    is CharacterResult.Failure -> {
                        _uiState.update {
                            it.copy(
                                isCharacterLoading = false,
                                characterError = result.error.toUiError(),
                            )
                        }
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            }
        }
    }

    private fun loadEpisodes(character: Character) {
        val episodeIds = character.episodeUrls.mapNotNull { url ->
            url.substringAfterLast('/').toIntOrNull()
        }.distinct()
        if (episodeIds.isEmpty()) {
            _uiState.update {
                it.copy(
                    episodes = emptyList(),
                    isEpisodesLoading = false,
                    episodeError = null,
                )
            }
            return
        }
        if (episodesJob?.isActive == true) return

        _uiState.update {
            it.copy(
                isEpisodesLoading = true,
                episodeError = null,
            )
        }
        episodesJob = viewModelScope.launch {
            try {
                when (val result = repository.getEpisodes(episodeIds)) {
                    is EpisodeResult.Success -> _uiState.update {
                        it.copy(
                            episodes = result.episodes,
                            isEpisodesLoading = false,
                            episodeError = null,
                        )
                    }

                    is EpisodeResult.Failure -> _uiState.update {
                        it.copy(
                            isEpisodesLoading = false,
                            episodeError = result.error.toUiError(),
                        )
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            }
        }
    }

    private fun CharacterRepositoryError.toUiError(): CharacterListUiError = when (this) {
        is CharacterRepositoryError.Http -> CharacterListUiError.Http(statusCode)
        CharacterRepositoryError.Connection -> CharacterListUiError.Connection
        CharacterRepositoryError.Serialization -> CharacterListUiError.Serialization
    }
}
