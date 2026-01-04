package com.aghatis.asmal.ui.quran

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aghatis.asmal.data.model.SurahDetailResponse
import com.aghatis.asmal.data.repository.QuranRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class QuranDetailUiState {
    object Loading : QuranDetailUiState()
    data class Success(val surah: SurahDetailResponse) : QuranDetailUiState()
    data class Error(val message: String) : QuranDetailUiState()
}

class QuranDetailViewModel(
    private val repository: QuranRepository,
    private val surahNo: Int
) : ViewModel() {

    private val _uiState = MutableStateFlow<QuranDetailUiState>(QuranDetailUiState.Loading)
    val uiState: StateFlow<QuranDetailUiState> = _uiState.asStateFlow()

    init {
        loadSurahDetail()
    }

    private fun loadSurahDetail() {
        viewModelScope.launch {
            _uiState.value = QuranDetailUiState.Loading
            repository.getSurahDetail(surahNo)
                .onSuccess { surah ->
                    _uiState.value = QuranDetailUiState.Success(surah)
                }
                .onFailure { error ->
                    _uiState.value = QuranDetailUiState.Error(error.message ?: "Unknown Error")
                }
        }
    }

    class Factory(
        private val repository: QuranRepository,
        private val surahNo: Int
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(QuranDetailViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return QuranDetailViewModel(repository, surahNo) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
