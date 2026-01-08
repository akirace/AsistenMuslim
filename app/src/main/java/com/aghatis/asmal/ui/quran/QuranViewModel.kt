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

sealed interface AudioPlaybackState {
    object Idle : AudioPlaybackState
    data class Loading(val surahNo: Int) : AudioPlaybackState
    data class Playing(val surahNo: Int) : AudioPlaybackState
    // Paused could be added later if needed
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

    private val _selectedQoriId = MutableStateFlow("1")
    val selectedQoriId: StateFlow<String> = _selectedQoriId.asStateFlow()

    private val _playbackState = MutableStateFlow<AudioPlaybackState>(AudioPlaybackState.Idle)
    val playbackState: StateFlow<AudioPlaybackState> = _playbackState.asStateFlow()

    // Player specific states
    private val _playbackProgress = MutableStateFlow(0f) // 0.0 to 1.0
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()
    
    private val _currentDuration = MutableStateFlow(0)
    val currentDuration: StateFlow<Int> = _currentDuration.asStateFlow()
    
    private val _currentPosition = MutableStateFlow(0)
    val currentPosition: StateFlow<Int> = _currentPosition.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: kotlinx.coroutines.Job? = null

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

    fun togglePlayPause() {
        mediaPlayer?.let { mp ->
            if (mp.isPlaying) {
                mp.pause()
                _isPlaying.value = false
                stopProgressUpdate()
                // Update state to Paused if we had a Paused state, or just keep Playing but isPlaying=false
            } else {
                mp.start()
                _isPlaying.value = true
                startProgressUpdate()
            }
        }
    }

    fun playNextSurah() {
        val currentSurahNo = (_playbackState.value as? AudioPlaybackState.Playing)?.surahNo 
            ?: (_playbackState.value as? AudioPlaybackState.Loading)?.surahNo 
            ?: return
            
        val nextSurahNo = currentSurahNo + 1
        if (nextSurahNo <= 114) {
            playAudio(nextSurahNo)
        }
    }

    fun playPreviousSurah() {
         val currentSurahNo = (_playbackState.value as? AudioPlaybackState.Playing)?.surahNo 
            ?: (_playbackState.value as? AudioPlaybackState.Loading)?.surahNo 
            ?: return
            
        val prevSurahNo = currentSurahNo - 1
        if (prevSurahNo >= 1) {
            playAudio(prevSurahNo)
        }
    }

    fun seekTo(progress: Float) {
        mediaPlayer?.let { mp ->
            val newPos = (mp.duration * progress).toInt()
            mp.seekTo(newPos)
            _currentPosition.value = newPos
            _playbackProgress.value = progress
        }
    }

    fun playAudio(surahNo: Int) {
        // If clicking the same surah that is playing
        val currentState = _playbackState.value
        if (currentState is AudioPlaybackState.Playing && currentState.surahNo == surahNo) {
            // Do nothing if already playing, maybe open player? 
            // The UI will handle navigation.
            return
        }
        
        stopAudio() // Stop any previous
        _playbackState.value = AudioPlaybackState.Loading(surahNo)
        _isPlaying.value = false
        _playbackProgress.value = 0f

        viewModelScope.launch {
            val result = repository.getSurahDetail(surahNo)
            result.onSuccess { detail ->
                // Check if we are still in loading state for this surah
                val current = _playbackState.value
                if (current !is AudioPlaybackState.Loading || current.surahNo != surahNo) {
                    return@onSuccess
                }

                val qoriId = _selectedQoriId.value
                val audioUrl = detail.audio[qoriId]?.url ?: detail.audio["1"]?.url
                
                if (audioUrl != null) {
                    playUrl(audioUrl, surahNo)
                } else {
                    _playbackState.value = AudioPlaybackState.Idle
                }
            }.onFailure {
                _playbackState.value = AudioPlaybackState.Idle
            }
        }
    }

    private fun playUrl(url: String, surahNo: Int) {
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(url)
                prepareAsync()
                setOnPreparedListener { mp ->
                    mp.start()
                    _playbackState.value = AudioPlaybackState.Playing(surahNo)
                    _isPlaying.value = true
                    _currentDuration.value = mp.duration
                    startProgressUpdate()
                }
                setOnCompletionListener {
                    _playbackState.value = AudioPlaybackState.Idle
                    _isPlaying.value = false
                    stopProgressUpdate()
                    _playbackProgress.value = 1f
                    // Auto play next? User didn't specify, but standard player behavior usually does.
                    // For now, stop.
                }
                setOnErrorListener { _, _, _ ->
                    _playbackState.value = AudioPlaybackState.Idle
                    _isPlaying.value = false
                    stopProgressUpdate()
                    true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _playbackState.value = AudioPlaybackState.Idle
            _isPlaying.value = false
        }
    }

    private fun startProgressUpdate() {
        stopProgressUpdate()
        progressJob = viewModelScope.launch {
            while (true) {
                mediaPlayer?.let { mp ->
                     if (mp.isPlaying) {
                         val current = mp.currentPosition
                         val total = mp.duration
                         if (total > 0) {
                             _currentPosition.value = current
                             _playbackProgress.value = current.toFloat() / total.toFloat()
                         }
                     }
                }
                kotlinx.coroutines.delay(100)
            }
        }
    }

    private fun stopProgressUpdate() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun stopAudio() {
        stopProgressUpdate()
        _isPlaying.value = false
        _playbackProgress.value = 0f
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaPlayer = null
        _playbackState.value = AudioPlaybackState.Idle
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

