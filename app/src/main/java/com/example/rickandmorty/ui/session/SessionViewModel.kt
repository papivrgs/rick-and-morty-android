package com.example.rickandmorty.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.rickandmorty.domain.repository.AuthRepository
import com.example.rickandmorty.domain.repository.SessionState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SessionViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {
    val sessionState: StateFlow<SessionState> = authRepository.sessionState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SessionState.Loading,
    )

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }
}

class SessionViewModelFactory(
    private val authRepository: AuthRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(SessionViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        @Suppress("UNCHECKED_CAST")
        return SessionViewModel(authRepository) as T
    }
}
