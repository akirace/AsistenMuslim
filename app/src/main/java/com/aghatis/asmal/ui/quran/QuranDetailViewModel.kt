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

import android.media.MediaPlayer

sealed class AudioState {
    object Idle : AudioState()
    data class Loading(val ayahNo: Int) : AudioState()
    data class Playing(val ayahNo: Int) : AudioState()
    data class Error(val message: String) : AudioState()
}

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

    private val _audioState = MutableStateFlow<AudioState>(AudioState.Idle)
    val audioState: StateFlow<AudioState> = _audioState.asStateFlow()

    private val _showTranslation = MutableStateFlow(true)
    val showTranslation: StateFlow<Boolean> = _showTranslation.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null

    init {
        loadSurahDetail()
    }

    fun toggleTranslation(show: Boolean) {
        _showTranslation.value = show
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

    fun playAyahAudio(ayahNo: Int) {
        // Stop currently playing
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null

        viewModelScope.launch {
            _audioState.value = AudioState.Loading(ayahNo)
            repository.getAyah(surahNo, ayahNo)
                .onSuccess { ayah ->
                    val url = ayah.audio["1"]?.url // Using Mishary Rashid as default
                    if (url != null) {
                        try {
                            mediaPlayer = MediaPlayer().apply {
                                setDataSource(url)
                                setOnPreparedListener {
                                    start()
                                    _audioState.value = AudioState.Playing(ayahNo)
                                }
                                setOnCompletionListener {
                                    _audioState.value = AudioState.Idle
                                }
                                setOnErrorListener { _, _, _ ->
                                    _audioState.value = AudioState.Error("Failed to play audio")
                                    false
                                }
                                prepareAsync()
                            }
                        } catch (e: Exception) {
                            _audioState.value = AudioState.Error(e.message ?: "Player Error")
                        }
                    } else {
                        _audioState.value = AudioState.Error("Audio URL not found")
                    }
                }
                .onFailure { error ->
                    _audioState.value = AudioState.Error(error.message ?: "Failed to fetch Ayah")
                }
        }
    }

    fun stopAudio() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
        _audioState.value = AudioState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
        mediaPlayer = null
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
