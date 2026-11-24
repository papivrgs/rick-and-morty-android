package com.example.rickandmorty.ui.characters.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.rickandmorty.domain.repository.CharacterRepository

class CharacterDetailViewModelFactory(
    private val characterId: Int,
    private val repository: CharacterRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(CharacterDetailViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return CharacterDetailViewModel(characterId, repository) as T
    }
}
