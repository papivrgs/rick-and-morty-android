package com.example.rickandmorty.ui.auth

import com.example.rickandmorty.domain.model.AuthenticatedUser
import com.example.rickandmorty.domain.repository.AuthRepository
import com.example.rickandmorty.domain.repository.AuthResult
import com.example.rickandmorty.domain.repository.SessionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelsTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `login validates required fields without calling repository`() = runTest(dispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = LoginViewModel(repository)

        viewModel.submit()

        assertEquals(AuthFieldError.Required, viewModel.uiState.value.usernameError)
        assertEquals(AuthFieldError.Required, viewModel.uiState.value.passwordError)
        assertEquals(0, repository.loginCalls)
    }

    @Test
    fun `login exposes invalid credentials and allows another attempt`() = runTest(dispatcher) {
        val repository = FakeAuthRepository(loginResult = AuthResult.InvalidCredentials)
        val viewModel = LoginViewModel(repository)
        viewModel.onUsernameChanged("Rick")
        viewModel.onPasswordChanged("wrong-password")

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(AuthUiError.InvalidCredentials, viewModel.uiState.value.authError)
        assertFalse(viewModel.uiState.value.isSubmitting)
        assertEquals(1, repository.loginCalls)
    }

    @Test
    fun `successful login does not expose an error`() = runTest(dispatcher) {
        val repository = FakeAuthRepository(loginResult = success)
        val viewModel = LoginViewModel(repository)
        viewModel.onUsernameChanged("Rick")
        viewModel.onPasswordChanged("password")

        viewModel.submit()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.authError)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun `registration requires eight characters and matching confirmation`() = runTest(dispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = RegisterViewModel(repository)
        viewModel.onUsernameChanged("Morty")
        viewModel.onPasswordChanged("short")
        viewModel.onPasswordConfirmationChanged("different")

        viewModel.submit()

        assertEquals(AuthFieldError.PasswordTooShort, viewModel.uiState.value.passwordError)
        assertEquals(
            AuthFieldError.PasswordsDoNotMatch,
            viewModel.uiState.value.passwordConfirmationError,
        )
        assertEquals(0, repository.registerCalls)
    }

    @Test
    fun `registration exposes a duplicate username`() = runTest(dispatcher) {
        val repository = FakeAuthRepository(registerResult = AuthResult.UsernameTaken)
        val viewModel = RegisterViewModel(repository)
        viewModel.onUsernameChanged("Summer")
        viewModel.onPasswordChanged("password")
        viewModel.onPasswordConfirmationChanged("password")

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(AuthUiError.UsernameTaken, viewModel.uiState.value.authError)
        assertEquals(1, repository.registerCalls)
    }

    @Test
    fun `registration submits valid fields`() = runTest(dispatcher) {
        val repository = FakeAuthRepository(registerResult = success)
        val viewModel = RegisterViewModel(repository)
        viewModel.onUsernameChanged("Summer")
        viewModel.onPasswordChanged("password")
        viewModel.onPasswordConfirmationChanged("password")

        viewModel.submit()
        advanceUntilIdle()

        assertTrue(repository.lastRegisteredCredentials == ("Summer" to "password"))
        assertNull(viewModel.uiState.value.authError)
    }

    private class FakeAuthRepository(
        private val loginResult: AuthResult = AuthResult.StorageError,
        private val registerResult: AuthResult = AuthResult.StorageError,
    ) : AuthRepository {
        override val sessionState: Flow<SessionState> = MutableStateFlow(SessionState.Unauthenticated)
        var loginCalls = 0
        var registerCalls = 0
        var lastRegisteredCredentials: Pair<String, String>? = null

        override suspend fun login(username: String, password: String): AuthResult {
            loginCalls++
            return loginResult
        }

        override suspend fun register(username: String, password: String): AuthResult {
            registerCalls++
            lastRegisteredCredentials = username to password
            return registerResult
        }

        override suspend fun logout() = Unit
    }

    private companion object {
        val success = AuthResult.Success(AuthenticatedUser(id = 1, username = "Rick"))
    }
}
