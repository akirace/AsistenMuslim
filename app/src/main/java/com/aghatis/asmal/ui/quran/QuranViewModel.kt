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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

sealed class QuranUiState {
    object Loading : QuranUiState()
    data class Success(val surahs: List<SurahEntity>) : QuranUiState()
    data class Error(val message: String) : QuranUiState()
}

class QuranViewModel(private val repository: QuranRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _allSurahs = MutableStateFlow<List<SurahEntity>>(emptyList())
    
    // We need to keep uiState as a simple StateFlow but derived from others
    // Using stateIn is more idiomatic for deriving flows in ViewModels
    private val _internalUiState = MutableStateFlow<QuranUiState>(QuranUiState.Loading)
    
    val uiState: StateFlow<QuranUiState> = combine(_allSurahs, _searchQuery) { surahs, query ->
        if (surahs.isEmpty()) {
            // Check if we are still loading or truly empty
            if (_internalUiState.value is QuranUiState.Loading) QuranUiState.Loading 
            else QuranUiState.Success(emptyList())
        } else if (query.isEmpty()) {
            QuranUiState.Success(surahs)
        } else {
            val filtered = surahs.filter { 
                it.surahName.contains(query, ignoreCase = true) || 
                it.surahNo.toString() == query ||
                it.surahNameArabic.contains(query)
            }
            QuranUiState.Success(filtered)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = QuranUiState.Loading
    )

    init {
        getSurahs()
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun getSurahs() {
        viewModelScope.launch {
            _internalUiState.value = QuranUiState.Loading
            repository.getSurahs()
                .catch { e ->
                    _internalUiState.value = QuranUiState.Error(e.message ?: "Unknown Error")
                }
                .collect { surahs ->
                    _allSurahs.value = surahs
                    _internalUiState.value = QuranUiState.Success(surahs)
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
