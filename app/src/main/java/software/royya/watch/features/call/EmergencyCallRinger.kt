package software.royya.watch.features.call

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plays an audible incoming-call ringtone and vibration while an emergency
 * alert is ringing before the farmer answers the call.
 */
@Singleton
class EmergencyCallRinger @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    fun startRinging() {
        stopRinging()
        startRingtoneAudio()
        startVibration()
    }

    fun stopRinging() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }
        } catch (_: Exception) {
            // Ignore cleanup errors on already-released MediaPlayer
        } finally {
            mediaPlayer = null
        }

        try {
            vibrator?.cancel()
        } catch (_: Exception) {
            // Ignore cleanup errors
        } finally {
            vibrator = null
        }
    }

    private fun startRingtoneAudio() {
        val candidateUris = listOfNotNull(
            resolveUri(RingtoneManager.TYPE_RINGTONE),
            resolveUri(RingtoneManager.TYPE_ALARM),
            resolveUri(RingtoneManager.TYPE_NOTIFICATION)
        )

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        for (uri in candidateUris) {
            try {
                val player = MediaPlayer().apply {
                    setAudioAttributes(attributes)
                    setDataSource(context, uri)
                    isLooping = true
                    prepare()
                    start()
                }
                mediaPlayer = player
                return
            } catch (_: Exception) {
                // Try next candidate ringtone/alarm URI
            }
        }
    }

    private fun resolveUri(type: Int): Uri? {
        return try {
            RingtoneManager.getActualDefaultRingtoneUri(context, type)
                ?: RingtoneManager.getDefaultUri(type)
        } catch (_: Exception) {
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun startVibration() {
        try {
            val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return

            if (!vib.hasVibrator()) return
            vibrator = vib

            val pattern = longArrayOf(0, 700, 500)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                vib.vibrate(pattern, 0)
            }
        } catch (_: Exception) {
            // Ignore if vibration service is unavailable
        }
    }
}
