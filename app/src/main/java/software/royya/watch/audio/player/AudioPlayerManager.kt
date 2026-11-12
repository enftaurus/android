package software.royya.watch.audio.player

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "RoyyaAudio"

sealed interface PlaybackState {
    data object Idle : PlaybackState
    data object Loading : PlaybackState
    data class Playing(val currentPositionMs: Long, val durationMs: Long) : PlaybackState
    data class Paused(val currentPositionMs: Long, val durationMs: Long) : PlaybackState
    data object Completed : PlaybackState
    data class Error(val message: String) : PlaybackState
}

@Singleton
class AudioPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var exoPlayer: ExoPlayer? = null

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var isPlayingFallback = false
    private var lastUseAlarmStream = false

    private fun bundledSampleUri(): Uri {
        return Uri.parse("android.resource://${context.packageName}/${software.royya.watch.R.raw.sample_alert_te}")
    }

    private fun resolveUri(uriString: String): Uri {
        return if (uriString.isBlank() || uriString.contains("/raw/sample_alert_te")) {
            isPlayingFallback = true
            bundledSampleUri()
        } else {
            isPlayingFallback = false
            Uri.parse(uriString)
        }
    }

    private fun buildAudioAttributes(useAlarmStream: Boolean): AudioAttributes {
        return AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .setUsage(if (useAlarmStream) C.USAGE_ALARM else C.USAGE_MEDIA)
            .build()
    }

    private fun getOrCreatePlayer(): ExoPlayer {
        return exoPlayer ?: ExoPlayer.Builder(context)
            .setAudioAttributes(buildAudioAttributes(useAlarmStream = false), false)
            .build().apply {
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        Log.d(TAG, "onPlaybackStateChanged state=$state isPlaying=$isPlaying suppression=$playbackSuppressionReason")
                        when (state) {
                            Player.STATE_BUFFERING -> _playbackState.value = PlaybackState.Loading
                            Player.STATE_READY -> {
                                if (isPlaying) {
                                    _playbackState.value = PlaybackState.Playing(currentPosition, duration)
                                } else {
                                    _playbackState.value = PlaybackState.Paused(currentPosition, duration)
                                }
                            }
                            Player.STATE_ENDED -> _playbackState.value = PlaybackState.Completed
                            Player.STATE_IDLE -> _playbackState.value = PlaybackState.Idle
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        Log.d(TAG, "onIsPlayingChanged isPlaying=$isPlaying state=$playbackState suppression=$playbackSuppressionReason")
                        if (isPlaying) {
                            _playbackState.value = PlaybackState.Playing(currentPosition, duration)
                        } else if (_playbackState.value is PlaybackState.Playing) {
                            _playbackState.value = PlaybackState.Paused(currentPosition, duration)
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        Log.e(TAG, "onPlayerError fallback=$isPlayingFallback code=${error.errorCodeName}: ${error.message}", error)
                        if (!isPlayingFallback) {
                            isPlayingFallback = true
                            try {
                                val fallbackUri = bundledSampleUri()
                                Log.d(TAG, "Switching to bundled fallback URI: $fallbackUri")
                                setAudioAttributes(buildAudioAttributes(lastUseAlarmStream), false)
                                setMediaItem(MediaItem.fromUri(fallbackUri))
                                prepare()
                                play()
                                return
                            } catch (e: Exception) {
                                Log.e(TAG, "Fallback playback failed", e)
                            }
                        }
                        _playbackState.value = PlaybackState.Error(error.localizedMessage ?: "Audio playback failed")
                    }
                })
                exoPlayer = this
            }
    }

    fun play(uriString: String, useAlarmStream: Boolean = false) {
        try {
            lastUseAlarmStream = useAlarmStream
            val uri = resolveUri(uriString)
            Log.d(TAG, "play() uri=$uri useAlarmStream=$useAlarmStream isFallback=$isPlayingFallback")
            val player = getOrCreatePlayer()
            player.stop()
            player.clearMediaItems()
            player.setAudioAttributes(buildAudioAttributes(useAlarmStream), false)
            _playbackState.value = PlaybackState.Loading
            val mediaItem = MediaItem.fromUri(uri)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
        } catch (e: Exception) {
            Log.e(TAG, "play() exception", e)
            _playbackState.value = PlaybackState.Error(e.message ?: "Failed to initialize playback")
        }
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun resume() {
        exoPlayer?.play()
    }

    fun stop() {
        exoPlayer?.stop()
        _playbackState.value = PlaybackState.Idle
    }

    fun release() {
        exoPlayer?.release()
        exoPlayer = null
        _playbackState.value = PlaybackState.Idle
    }
}
