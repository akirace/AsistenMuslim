package com.aghatis.asmal.ui.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aghatis.asmal.data.repository.AuthRepository
import com.aghatis.asmal.data.repository.PrefsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MenuViewModel(
    private val authRepository: AuthRepository,
    private val prefsRepository: PrefsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MenuUiState>(MenuUiState.Idle)
    val uiState: StateFlow<MenuUiState> = _uiState.asStateFlow()

    fun logout() {
        viewModelScope.launch {
            _uiState.value = MenuUiState.Loading
            authRepository.signOut()
            prefsRepository.clearSession()
            // Add a small delay to ensure UI updates or just proceed
            _uiState.value = MenuUiState.LoggedOut
        }
    }

    companion object {
        fun Factory(
            authRepository: AuthRepository,
            prefsRepository: PrefsRepository
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                MenuViewModel(authRepository, prefsRepository)
            }
        }
    }
}

sealed class MenuUiState {
    object Idle : MenuUiState()
    object Loading : MenuUiState()
    object LoggedOut : MenuUiState()
}
