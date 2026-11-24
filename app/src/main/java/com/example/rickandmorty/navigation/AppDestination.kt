package com.example.rickandmorty.navigation

sealed class AppDestination(val route: String) {
    data object Login : AppDestination("login")

    data object Register : AppDestination("register")

    data object Characters : AppDestination("characters")
}
