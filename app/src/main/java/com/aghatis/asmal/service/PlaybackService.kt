package com.aghatis.asmal.service

import android.content.Intent
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.ForwardingPlayer
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.common.MediaItem
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.Futures
import com.aghatis.asmal.data.repository.QuranRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private lateinit var repository: QuranRepository
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        repository = QuranRepository(this)
        val player = ExoPlayer.Builder(this).build()
        
        val forwardingPlayer = object : ForwardingPlayer(player) {
            override fun getAvailableCommands(): Player.Commands {
                return super.getAvailableCommands().buildUpon()
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .build()
            }
        }

        mediaSession = MediaSession.Builder(this, forwardingPlayer)
            .setCallback(CustomCallback())
            .build()
    }

    private inner class CustomCallback : MediaSession.Callback {
        @OptIn(UnstableApi::class)
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(SessionCommand(Player.COMMAND_SEEK_TO_NEXT.toString(), Bundle.EMPTY))
                .add(SessionCommand(Player.COMMAND_SEEK_TO_PREVIOUS.toString(), Bundle.EMPTY))
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .build()
        }

        @Deprecated("Deprecated in Java")
        override fun onPlayerCommandRequest(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            playerCommand: Int
        ): Int {
            if (playerCommand == Player.COMMAND_SEEK_TO_NEXT || playerCommand == Player.COMMAND_SEEK_TO_PREVIOUS) {
                handleSkip(playerCommand == Player.COMMAND_SEEK_TO_NEXT)
                return SessionResult.RESULT_SUCCESS
            }
            return super.onPlayerCommandRequest(session, controller, playerCommand)
        }
    }

    private fun handleSkip(isNext: Boolean) {
        val player = mediaSession?.player ?: return
        val currentSurahNo = player.currentMediaItem?.mediaMetadata?.extras?.getInt("surahNo") ?: 1
        val nextSurahNo = if (isNext) currentSurahNo + 1 else currentSurahNo - 1
        
        if (nextSurahNo in 1..114) {
            serviceScope.launch {
                repository.getSurahDetail(nextSurahNo).onSuccess { detail ->
                    val audioData = detail.audio["1"] // Default Mishary Rashid
                    val url = audioData?.url
                    if (url != null) {
                        val extras = Bundle().apply { putInt("surahNo", nextSurahNo) }
                        val mediaItem = MediaItem.Builder()
                            .setUri(url)
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle(detail.surahName)
                                    .setArtist(audioData?.reciter ?: "Unknown Reciter")
                                    .setExtras(extras)
                                    .build()
                            )
                            .build()
                        player.setMediaItem(mediaItem)
                        player.prepare()
                        player.play()
                    }
                }
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
