package com.example.rickandmorty.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.rickandmorty.R
import com.example.rickandmorty.app.AppContainer
import com.example.rickandmorty.domain.repository.SessionState
import com.example.rickandmorty.ui.auth.LoginScreen
import com.example.rickandmorty.ui.auth.LoginViewModel
import com.example.rickandmorty.ui.auth.LoginViewModelFactory
import com.example.rickandmorty.ui.auth.RegisterScreen
import com.example.rickandmorty.ui.auth.RegisterViewModel
import com.example.rickandmorty.ui.auth.RegisterViewModelFactory
import com.example.rickandmorty.ui.characters.CharacterListScreen
import com.example.rickandmorty.ui.characters.CharacterListViewModel
import com.example.rickandmorty.ui.characters.CharacterListViewModelFactory
import com.example.rickandmorty.ui.session.SessionViewModel
import com.example.rickandmorty.ui.session.SessionViewModelFactory

@Composable
fun RickAndMortyApp(
    appContainer: AppContainer,
    modifier: Modifier = Modifier,
) {
    val sessionFactory = remember(appContainer.authRepository) {
        SessionViewModelFactory(appContainer.authRepository)
    }
    val sessionViewModel = viewModel<SessionViewModel>(factory = sessionFactory)
    val sessionState by sessionViewModel.sessionState.collectAsStateWithLifecycle()

    when (val state = sessionState) {
        SessionState.Loading -> SessionLoadingScreen(modifier)
        SessionState.Unauthenticated -> key("unauthenticated") {
            AuthenticationNavHost(
                appContainer = appContainer,
                modifier = modifier,
            )
        }

        is SessionState.Authenticated -> key("authenticated-${state.user.id}") {
            AuthenticatedNavHost(
                appContainer = appContainer,
                onLogout = sessionViewModel::logout,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun AuthenticationNavHost(
    appContainer: AppContainer,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val loginFactory = remember(appContainer.authRepository) {
        LoginViewModelFactory(appContainer.authRepository)
    }
    val registerFactory = remember(appContainer.authRepository) {
        RegisterViewModelFactory(appContainer.authRepository)
    }
    NavHost(
        navController = navController,
        startDestination = AppDestination.Login.route,
        modifier = modifier,
    ) {
        composable(AppDestination.Login.route) {
            val loginViewModel = viewModel<LoginViewModel>(factory = loginFactory)
            LoginScreen(
                viewModel = loginViewModel,
                onCreateAccount = { navController.navigate(AppDestination.Register.route) },
            )
        }
        composable(AppDestination.Register.route) {
            val registerViewModel = viewModel<RegisterViewModel>(factory = registerFactory)
            RegisterScreen(
                viewModel = registerViewModel,
                onBackToLogin = { navController.popBackStack() },
            )
        }
    }
}

@Composable
private fun AuthenticatedNavHost(
    appContainer: AppContainer,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val characterFactory = remember(appContainer.characterRepository) {
        CharacterListViewModelFactory(appContainer.characterRepository)
    }
    NavHost(
        navController = navController,
        startDestination = AppDestination.Characters.route,
        modifier = modifier,
    ) {
        composable(AppDestination.Characters.route) {
            val characterViewModel = viewModel<CharacterListViewModel>(factory = characterFactory)
            CharacterListScreen(
                viewModel = characterViewModel,
                onLogout = onLogout,
            )
        }
    }
}

@Composable
private fun SessionLoadingScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        CircularProgressIndicator()
        Text(
            text = stringResource(R.string.session_loading),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
