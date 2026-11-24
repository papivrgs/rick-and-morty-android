package com.example.rickandmorty.navigation

sealed class AppDestination(val route: String) {
    data object Login : AppDestination("login")

    data object Register : AppDestination("register")

    data object Characters : AppDestination("characters")

    data object CharacterDetail : AppDestination("characters/{characterId}") {
        const val CHARACTER_ID_ARGUMENT = "characterId"

        fun createRoute(characterId: Int) = "characters/$characterId"
    }
}
