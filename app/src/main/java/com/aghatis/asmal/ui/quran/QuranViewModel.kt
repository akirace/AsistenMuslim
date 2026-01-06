package com.aghatis.asmal.ui.quran

import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aghatis.asmal.data.model.SurahEntity
import com.aghatis.asmal.data.repository.QuranRepository
import com.aghatis.asmal.data.repository.QoriRepository
import com.aghatis.asmal.data.model.QoriEntity
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

class QuranViewModel(
    private val repository: QuranRepository,
    private val qoriRepository: QoriRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Qori UI State
    private val _qoriList = MutableStateFlow<List<QoriEntity>>(emptyList())
    val qoriList: StateFlow<List<QoriEntity>> = _qoriList.asStateFlow()

    // Audio Playback State
    private val _selectedQoriId = MutableStateFlow("1")
    val selectedQoriId: StateFlow<String> = _selectedQoriId.asStateFlow()

    private val _currentPlayingSurah = MutableStateFlow<Int?>(null)
    val currentPlayingSurah: StateFlow<Int?> = _currentPlayingSurah.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null

    private val _allSurahs = MutableStateFlow<List<SurahEntity>>(emptyList())

    // We need to keep uiState as a simple StateFlow but derived from others
    // Using stateIn is more idiomatic for deriving flows in ViewModels
    private val _internalUiState = MutableStateFlow<QuranUiState>(QuranUiState.Loading)

    // We need to expose all surahs for the list in Audio screen
    val allSurahs: StateFlow<List<SurahEntity>> = _allSurahs.asStateFlow()
    
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
        getQoris()
    }

    private fun getQoris() {
        viewModelScope.launch {
            qoriRepository.getQoris().collect { qoris ->
                _qoriList.value = qoris
            }
        }
    }

    fun onSelectQori(id: String) {
        _selectedQoriId.value = id
        // Stop playing if qori changes? Optional. Let's stop to avoid confusion.
        stopAudio()
    }

    fun playAudio(surahNo: Int) {
        // If clicking the same surah that is playing, toggle (stop)
        if (_currentPlayingSurah.value == surahNo) {
            stopAudio()
            return
        }

        stopAudio() // Stop any previous
        _currentPlayingSurah.value = surahNo // Set loading/playing state indicator

        viewModelScope.launch {
            val result = repository.getSurahDetail(surahNo)
            result.onSuccess { detail ->
                val qoriId = _selectedQoriId.value
                // Fallback to "1" if selected not found, though UI should prevent this
                val audioUrl = detail.audio[qoriId]?.url ?: detail.audio["1"]?.url
                
                if (audioUrl != null) {
                    playUrl(audioUrl)
                } else {
                    _currentPlayingSurah.value = null // Reset if url not found
                    // Could expose error state here
                }
            }.onFailure {
                _currentPlayingSurah.value = null
            }
        }
    }

    private fun playUrl(url: String) {
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(url)
                prepareAsync()
                setOnPreparedListener { mp ->
                    mp.start()
                }
                setOnCompletionListener {
                    _currentPlayingSurah.value = null
                }
                setOnErrorListener { _, _, _ ->
                    _currentPlayingSurah.value = null
                    true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _currentPlayingSurah.value = null
        }
    }

    private fun stopAudio() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaPlayer = null
        _currentPlayingSurah.value = null
    }

    override fun onCleared() {
        super.onCleared()
        stopAudio()
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

    class Factory(
        private val repository: QuranRepository,
        private val qoriRepository: QoriRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(QuranViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return QuranViewModel(repository, qoriRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

