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

    private val _quickAccessIds = MutableStateFlow<List<String>>(listOf("quran", "qibla"))
    val quickAccessIds: StateFlow<List<String>> = _quickAccessIds.asStateFlow()

    init {
        viewModelScope.launch {
            prefsRepository.isLoggedIn.collect { isLoggedIn ->
                if (!isLoggedIn) {
                    _uiState.value = MenuUiState.LoggedOut
                }
            }
        }

        viewModelScope.launch {
            prefsRepository.quickAccessItems.collect { items ->
                if (items != null) {
                    _quickAccessIds.value = items.split(",").filter { it.isNotEmpty() }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.value = MenuUiState.Loading
            authRepository.signOut()
            prefsRepository.clearSession()
        }
    }

    fun toggleQuickAccess(id: String) {
        val current = _quickAccessIds.value.toMutableList()
        if (current.contains(id)) {
            if (current.size > 1) { // Keep at least one
                current.remove(id)
            }
        } else {
            if (current.size < 4) { // Limit to 4 for UI neatness
                current.add(id)
            }
        }
        
        viewModelScope.launch {
            _quickAccessIds.value = current
            prefsRepository.saveQuickAccessItems(current.joinToString(","))
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
