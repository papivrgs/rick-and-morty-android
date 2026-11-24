package com.example.rickandmorty.domain.repository

import com.example.rickandmorty.domain.model.AuthenticatedUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val sessionState: Flow<SessionState>

    suspend fun login(username: String, password: String): AuthResult

    suspend fun register(username: String, password: String): AuthResult

    suspend fun logout()
}

sealed interface SessionState {
    data object Loading : SessionState

    data class Authenticated(val user: AuthenticatedUser) : SessionState

    data object Unauthenticated : SessionState
}

sealed interface AuthResult {
    data class Success(val user: AuthenticatedUser) : AuthResult

    data object InvalidCredentials : AuthResult

    data object UsernameTaken : AuthResult

    data object StorageError : AuthResult
}
