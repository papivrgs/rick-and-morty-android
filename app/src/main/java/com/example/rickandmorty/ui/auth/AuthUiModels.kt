package com.example.rickandmorty.ui.auth

enum class AuthFieldError {
    Required,
    PasswordTooShort,
    PasswordsDoNotMatch,
}

enum class AuthUiError {
    InvalidCredentials,
    UsernameTaken,
    Storage,
}

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val usernameError: AuthFieldError? = null,
    val passwordError: AuthFieldError? = null,
    val authError: AuthUiError? = null,
    val isSubmitting: Boolean = false,
)

data class RegisterUiState(
    val username: String = "",
    val password: String = "",
    val passwordConfirmation: String = "",
    val usernameError: AuthFieldError? = null,
    val passwordError: AuthFieldError? = null,
    val passwordConfirmationError: AuthFieldError? = null,
    val authError: AuthUiError? = null,
    val isSubmitting: Boolean = false,
)
