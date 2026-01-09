package com.aghatis.asmal.ui.quran

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import android.content.ComponentName
import com.aghatis.asmal.service.PlaybackService
import com.google.common.util.concurrent.MoreExecutors
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
    data class Buffering(val surahNo: Int) : AudioPlaybackState
    data class Playing(val surahNo: Int) : AudioPlaybackState
}

class QuranViewModel(
    private val repository: QuranRepository,
    private val qoriRepository: QoriRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _qoriList = MutableStateFlow<List<QoriEntity>>(emptyList())
    val qoriList: StateFlow<List<QoriEntity>> = _qoriList.asStateFlow()

    private val _selectedQoriId = MutableStateFlow("1")
    val selectedQoriId: StateFlow<String> = _selectedQoriId.asStateFlow()

    private val _playbackState = MutableStateFlow<AudioPlaybackState>(AudioPlaybackState.Idle)
    val playbackState: StateFlow<AudioPlaybackState> = _playbackState.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    private val _bufferedProgress = MutableStateFlow(0f)
    val bufferedProgress: StateFlow<Float> = _bufferedProgress.asStateFlow()
    
    private val _currentDuration = MutableStateFlow(0)
    val currentDuration: StateFlow<Int> = _currentDuration.asStateFlow()
    
    private val _currentPosition = MutableStateFlow(0)
    val currentPosition: StateFlow<Int> = _currentPosition.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private var player: Player? = null
    private var progressJob: kotlinx.coroutines.Job? = null

    private val _allSurahs = MutableStateFlow<List<SurahEntity>>(emptyList())
    private val _internalUiState = MutableStateFlow<QuranUiState>(QuranUiState.Loading)

    val allSurahs: StateFlow<List<SurahEntity>> = _allSurahs.asStateFlow()
    
    val uiState: StateFlow<QuranUiState> = combine(_allSurahs, _searchQuery) { surahs, query ->
        if (surahs.isEmpty()) {
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
        setupPlayer()
    }

    private fun setupPlayer() {
        val sessionToken = SessionToken(repository.context, ComponentName(repository.context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(repository.context, sessionToken).buildAsync()
        controllerFuture.addListener({
            try {
                val controller = controllerFuture.get()
                player = controller
                controller.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                        if (isPlaying) startProgressUpdate() else stopProgressUpdate()
                    }

                    override fun onPlaybackStateChanged(state: Int) {
                        when (state) {
                            Player.STATE_BUFFERING -> {
                                val surahNo = (_playbackState.value as? AudioPlaybackState.Loading)?.surahNo
                                    ?: (_playbackState.value as? AudioPlaybackState.Playing)?.surahNo
                                    ?: (_playbackState.value as? AudioPlaybackState.Buffering)?.surahNo
                                    ?: 0
                                if (surahNo > 0) _playbackState.value = AudioPlaybackState.Buffering(surahNo)
                            }
                            Player.STATE_READY -> {
                                _currentDuration.value = controller.duration.toInt()
                                val surahNo = (_playbackState.value as? AudioPlaybackState.Loading)?.surahNo
                                    ?: (_playbackState.value as? AudioPlaybackState.Buffering)?.surahNo
                                    ?: 0
                                if (surahNo > 0) _playbackState.value = AudioPlaybackState.Playing(surahNo)
                            }
                            Player.STATE_ENDED -> {
                                _playbackState.value = AudioPlaybackState.Idle
                                _isPlaying.value = false
                                stopProgressUpdate()
                                _playbackProgress.value = 1f
                                _bufferedProgress.value = 1f
                            }
                            Player.STATE_IDLE -> {
                                _playbackState.value = AudioPlaybackState.Idle
                            }
                        }
                    }
                    
                    // Handle Next/Previous from external (notification)
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        // If we use playlist, update surahNo here.
                        // For now we handle via playAudio.
                    }
                })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
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
        stopAudio()
    }

    fun togglePlayPause() {
        player?.let { p ->
            if (p.isPlaying) {
                p.pause()
            } else {
                if (p.playbackState == Player.STATE_ENDED) {
                    p.seekTo(0)
                }
                p.play()
            }
        }
    }

    fun playNextSurah() {
        val currentSurahNo = (_playbackState.value as? AudioPlaybackState.Playing)?.surahNo 
            ?: (_playbackState.value as? AudioPlaybackState.Loading)?.surahNo 
            ?: (_playbackState.value as? AudioPlaybackState.Buffering)?.surahNo 
            ?: return
            
        val nextSurahNo = currentSurahNo + 1
        if (nextSurahNo <= 114) {
            playAudio(nextSurahNo)
        }
    }

    fun playPreviousSurah() {
         val currentSurahNo = (_playbackState.value as? AudioPlaybackState.Playing)?.surahNo 
            ?: (_playbackState.value as? AudioPlaybackState.Loading)?.surahNo 
            ?: (_playbackState.value as? AudioPlaybackState.Buffering)?.surahNo 
            ?: return
            
        val prevSurahNo = currentSurahNo - 1
        if (prevSurahNo >= 1) {
            playAudio(prevSurahNo)
        }
    }

    fun seekTo(progress: Float) {
        player?.let { p ->
            val newPos = (p.duration * progress).toLong()
            p.seekTo(newPos)
            _currentPosition.value = newPos.toInt()
            _playbackProgress.value = progress
        }
    }

    fun playAudio(surahNo: Int) {
        val currentState = _playbackState.value
        if (currentState is AudioPlaybackState.Playing && currentState.surahNo == surahNo) {
            return
        }
        
        stopAudio()
        _playbackState.value = AudioPlaybackState.Loading(surahNo)
        _playbackProgress.value = 0f

        viewModelScope.launch {
            val result = repository.getSurahDetail(surahNo)
            result.onSuccess { detail ->
                val current = _playbackState.value
                if (current !is AudioPlaybackState.Loading || current.surahNo != surahNo) {
                    return@onSuccess
                }

                val qoriId = _selectedQoriId.value
                val audioUrl = detail.audio[qoriId]?.url ?: detail.audio["1"]?.url
                
                if (audioUrl != null) {
                    val reciterName = detail.audio[qoriId]?.reciter ?: detail.audio["1"]?.reciter ?: "Unknown"
                    playUrl(audioUrl, detail.surahName, reciterName, surahNo)
                } else {
                    _playbackState.value = AudioPlaybackState.Idle
                }
            }.onFailure {
                _playbackState.value = AudioPlaybackState.Idle
            }
        }
    }

    private fun playUrl(url: String, title: String, artist: String, surahNo: Int) {
        player?.let { p ->
            val extras = android.os.Bundle().apply {
                putInt("surahNo", surahNo)
            }
            val mediaItem = MediaItem.Builder()
                .setUri(url)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(title)
                        .setArtist(artist)
                        .setExtras(extras)
                        .build()
                )
                .build()
            p.setMediaItem(mediaItem)
            p.prepare()
            p.play()
        }
    }

    private fun startProgressUpdate() {
        stopProgressUpdate()
        progressJob = viewModelScope.launch {
            while (true) {
                player?.let { p ->
                    val current = p.currentPosition
                    val buffered = p.bufferedPosition
                    val total = p.duration
                    if (total > 0) {
                        _currentPosition.value = current.toInt()
                        _playbackProgress.value = current.toFloat() / total.toFloat()
                        _bufferedProgress.value = buffered.toFloat() / total.toFloat()
                    }
                }
                kotlinx.coroutines.delay(200)
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
        player?.stop()
        _playbackState.value = AudioPlaybackState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        stopAudio()
        (player as? MediaController)?.release()
        player = null
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

