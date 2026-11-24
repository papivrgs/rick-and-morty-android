package com.example.rickandmorty.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rickandmorty.domain.repository.AuthRepository
import com.example.rickandmorty.domain.repository.AuthResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onUsernameChanged(username: String) {
        _uiState.update {
            it.copy(username = username, usernameError = null, authError = null)
        }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update {
            it.copy(password = password, passwordError = null, authError = null)
        }
    }

    fun submit() {
        val current = _uiState.value
        if (current.isSubmitting) return

        val usernameError = AuthFieldError.Required.takeIf { current.username.isBlank() }
        val passwordError = AuthFieldError.Required.takeIf { current.password.isBlank() }
        if (usernameError != null || passwordError != null) {
            _uiState.update {
                it.copy(usernameError = usernameError, passwordError = passwordError)
            }
            return
        }

        _uiState.update { it.copy(isSubmitting = true, authError = null) }
        viewModelScope.launch {
            try {
                val result = authRepository.login(current.username, current.password)
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        authError = result.toUiError(),
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            }
        }
    }

    private fun AuthResult.toUiError(): AuthUiError? = when (this) {
        is AuthResult.Success -> null
        AuthResult.InvalidCredentials -> AuthUiError.InvalidCredentials
        AuthResult.UsernameTaken -> AuthUiError.UsernameTaken
        AuthResult.StorageError -> AuthUiError.Storage
    }
}
