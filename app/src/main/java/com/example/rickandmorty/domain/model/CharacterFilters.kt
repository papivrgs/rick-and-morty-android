package com.example.rickandmorty.domain.model

data class CharacterFilters(
    val name: String = "",
    val status: CharacterStatusFilter = CharacterStatusFilter.All,
    val species: String = "",
) {
    val hasActiveFilters: Boolean
        get() = name.isNotBlank() || status != CharacterStatusFilter.All || species.isNotBlank()

    fun normalized() = copy(
        name = name.trim(),
        species = species.trim(),
    )
}

enum class CharacterStatusFilter {
    All,
    Alive,
    Dead,
    Unknown,
}
