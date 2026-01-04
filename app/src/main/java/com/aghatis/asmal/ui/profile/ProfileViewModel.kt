package com.aghatis.asmal.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aghatis.asmal.data.model.User
import com.aghatis.asmal.data.repository.AuthRepository
import com.aghatis.asmal.data.repository.PrefsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val prefsRepository: PrefsRepository
) : ViewModel() {

    val userState: StateFlow<User?> = prefsRepository.userData
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun logout() {
        viewModelScope.launch {
            authRepository.signOut()
            prefsRepository.clearSession()
        }
    }

    companion object {
        fun Factory(
            authRepository: AuthRepository,
            prefsRepository: PrefsRepository
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ProfileViewModel(authRepository, prefsRepository)
            }
        }
    }
}
