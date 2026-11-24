package com.example.rickandmorty

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rickandmorty.app.AppContainer
import com.example.rickandmorty.ui.characters.CharacterListScreen
import com.example.rickandmorty.ui.characters.CharacterListViewModel
import com.example.rickandmorty.ui.characters.CharacterListViewModelFactory
import com.example.rickandmorty.ui.theme.RickAndMortyAppTheme

class MainActivity : ComponentActivity() {

    private val appContainer by lazy { AppContainer() }

    private val characterListViewModelFactory by lazy {
        CharacterListViewModelFactory(appContainer.characterRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RickAndMortyAppTheme {
                val characterListViewModel = viewModel<CharacterListViewModel>(
                    factory = characterListViewModelFactory,
                )
                CharacterListScreen(viewModel = characterListViewModel)
            }
        }
    }
}
