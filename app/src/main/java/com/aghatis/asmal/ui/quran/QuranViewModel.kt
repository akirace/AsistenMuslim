package com.aghatis.asmal.ui.quran

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aghatis.asmal.data.model.SurahEntity
import com.aghatis.asmal.data.repository.QuranRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

sealed class QuranUiState {
    object Loading : QuranUiState()
    data class Success(val surahs: List<SurahEntity>) : QuranUiState()
    data class Error(val message: String) : QuranUiState()
}

class QuranViewModel(private val repository: QuranRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<QuranUiState>(QuranUiState.Loading)
    val uiState: StateFlow<QuranUiState> = _uiState.asStateFlow()

    init {
        getSurahs()
    }

    fun getSurahs() {
        viewModelScope.launch {
            _uiState.value = QuranUiState.Loading
            repository.getSurahs()
                .catch { e ->
                    _uiState.value = QuranUiState.Error(e.message ?: "Unknown Error")
                }
                .collect { surahs ->
                    if (surahs.isNotEmpty()) {
                        _uiState.value = QuranUiState.Success(surahs)
                    } else {
                        // If empty but no error, it might be still loading from network inside repo logic
                        // But repo logic uses db.insertAll which emits to flow.
                        // If flow emits empty initially, we stick to Loading or Empty state.
                        // For now, if empty after collect, it probably means fetch failed silently or DB is empty.
                        // But since repo handles fetch-if-empty, valid flow emission should come.
                    }
                }
        }
    }

    class Factory(private val repository: QuranRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(QuranViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return QuranViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
