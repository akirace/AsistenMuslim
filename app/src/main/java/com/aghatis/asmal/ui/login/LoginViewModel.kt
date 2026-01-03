package com.aghatis.asmal.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aghatis.asmal.data.model.User
import com.aghatis.asmal.data.repository.AuthRepository
import com.aghatis.asmal.data.repository.PrefsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val prefsRepository: PrefsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        checkSession()
    }

    private fun checkSession() {
        viewModelScope.launch {
            val isLoggedIn = prefsRepository.isLoggedIn.first()
            if (isLoggedIn) {
                _uiState.value = LoginUiState.Success
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            val result = authRepository.firebaseAuthWithGoogle(idToken)
            result.onSuccess { firebaseUser ->
                val user = User(
                    id = firebaseUser.uid,
                    displayName = firebaseUser.displayName ?: "Unknown",
                    email = firebaseUser.email ?: "",
                    photoUrl = firebaseUser.photoUrl?.toString()
                )
                prefsRepository.saveUserSession(user)
                _uiState.value = LoginUiState.Success
            }.onFailure { e ->
                _uiState.value = LoginUiState.Error(e.message ?: "Login Failed")
            }
        }
    }

    companion object {
        fun Factory(
            authRepository: AuthRepository,
            prefsRepository: PrefsRepository
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                LoginViewModel(authRepository, prefsRepository)
            }
        }
    }
}

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    object Success : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}
