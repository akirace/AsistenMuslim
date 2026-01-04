package com.aghatis.asmal.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aghatis.asmal.data.model.AppTheme
import com.aghatis.asmal.data.repository.PrefsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingViewModel(private val prefsRepository: PrefsRepository) : ViewModel() {

    val selectedTheme: StateFlow<AppTheme> = prefsRepository.selectedTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppTheme.MATERIAL_YOU)

    fun updateTheme(theme: AppTheme) {
        viewModelScope.launch {
            prefsRepository.saveTheme(theme)
        }
    }

    class Factory(private val prefsRepository: PrefsRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return SettingViewModel(prefsRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
