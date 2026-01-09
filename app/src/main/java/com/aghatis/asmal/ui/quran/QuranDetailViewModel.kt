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

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

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

    private var exoPlayer: ExoPlayer? = null

    init {
        loadSurahDetail()
        setupPlayer()
    }

    private fun setupPlayer() {
        exoPlayer = ExoPlayer.Builder(repository.context).build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            _audioState.value = AudioState.Playing(currentAyahPlaying)
                        }
                        Player.STATE_ENDED -> {
                            _audioState.value = AudioState.Idle
                        }
                        else -> {
                            // Other states
                        }
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    _audioState.value = AudioState.Error(error.message ?: "Playback Error")
                }
            })
        }
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

    private var currentAyahPlaying: Int = 0

    fun playAyahAudio(ayahNo: Int) {
        currentAyahPlaying = ayahNo
        viewModelScope.launch {
            _audioState.value = AudioState.Loading(ayahNo)
            repository.getAyah(surahNo, ayahNo)
                .onSuccess { ayah ->
                    val url = ayah.audio["1"]?.url // Using Mishary Rashid as default
                    if (url != null) {
                        exoPlayer?.let { player ->
                            val mediaItem = MediaItem.fromUri(url)
                            // We use a custom "tag" to pass ayahNo to listener if needed, 
                            // but actually we can just update state here if we want simple logic.
                            player.setMediaItem(mediaItem)
                            // Setting tag manually is not direct in MediaItem, but we can use metadata or just trust the call order.
                            // Better: use the player state to update UI.
                            // To keep it simple, we use a class var for currentAyah
                            player.prepare()
                            player.play()
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
        exoPlayer?.stop()
        _audioState.value = AudioState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        exoPlayer?.release()
        exoPlayer = null
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
