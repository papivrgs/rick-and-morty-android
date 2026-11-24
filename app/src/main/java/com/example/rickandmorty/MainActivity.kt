package com.example.rickandmorty

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.rickandmorty.app.AppContainer
import com.example.rickandmorty.navigation.RickAndMortyApp
import com.example.rickandmorty.ui.theme.RickAndMortyAppTheme

class MainActivity : ComponentActivity() {

    private val appContainer by lazy { AppContainer(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RickAndMortyAppTheme {
                RickAndMortyApp(appContainer = appContainer)
            }
        }
    }
}
