package software.royya.watch.notifications.channels

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import software.royya.watch.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationChannelManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_CRITICAL = "channel_critical_alerts"
        const val CHANNEL_STANDARD = "channel_standard_alerts"
    }

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Critical Alert Channel (Heads-Up, Alarm sound, Vibration)
            val criticalChannel = NotificationChannel(
                CHANNEL_CRITICAL,
                context.getString(R.string.urgent_alert_title),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical low dissolved oxygen and aerator failure emergency alarms"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                setBypassDnd(true)
            }

            // Standard Channel
            val standardChannel = NotificationChannel(
                CHANNEL_STANDARD,
                context.getString(R.string.alerts_title),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Standard water quality warnings and telemetry updates"
                enableVibration(false)
            }

            notificationManager.createNotificationChannels(listOf(criticalChannel, standardChannel))
        }
    }
}
