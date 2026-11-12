package software.royya.watch.notifications.handlers

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import software.royya.watch.BuildConfig
import software.royya.watch.R
import software.royya.watch.data.remote.dto.ActiveAlertDto
import software.royya.watch.data.repository.RemoteAlertRepository
import software.royya.watch.features.call.EmergencyCallActivity
import software.royya.watch.notifications.channels.NotificationChannelManager
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmergencyCallLauncher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val remoteAlertRepository: RemoteAlertRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val started = AtomicBoolean(false)
    private var lastLaunchedAlertId: String? = null

    fun startMonitoring() {
        if (!BuildConfig.DATA_SOURCE.equals("REMOTE", ignoreCase = true)) return
        if (!started.compareAndSet(false, true)) return

        remoteAlertRepository.ensurePollingStarted()

        scope.launch {
            remoteAlertRepository.activeEmergencyAlert.collect { alert ->
                if (alert == null) {
                    lastLaunchedAlertId = null
                } else if (alert.id != lastLaunchedAlertId) {
                    lastLaunchedAlertId = alert.id
                    launchEmergencyCallOverlay(alert)
                }
            }
        }
    }

    fun launchEmergencyCallOverlay(alert: ActiveAlertDto) {
        val intent = EmergencyCallActivity.createIntent(
            context = context,
            alertId = alert.id,
            title = alert.title,
            message = alert.message,
            dissolvedOxygen = alert.dissolvedOxygen,
            audioUrl = alert.audioUrl
        )

        // Post full-screen intent notification for lock-screen / background resilience
        postFullScreenEmergencyNotification(alert, intent)

        // Launch directly as Activity (works in foreground and in background when SYSTEM_ALERT_WINDOW is granted)
        try {
            if (Settings.canDrawOverlays(context) || isAppInForeground()) {
                context.startActivity(intent)
            }
        } catch (_: Exception) {
            // Notification fullScreenIntent serves as fallback
        }
    }

    private fun isAppInForeground(): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
            ?: return false
        val appProcesses = activityManager.runningAppProcesses ?: return false
        val packageName = context.packageName
        return appProcesses.any {
            it.importance == android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                it.processName == packageName
        }
    }

    private fun postFullScreenEmergencyNotification(
        alert: ActiveAlertDto,
        fullScreenIntent: android.content.Intent
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            alert.id.hashCode(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationChannelManager.CHANNEL_CRITICAL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(alert.title)
            .setContentText(alert.message)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setOngoing(true)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_EMERGENCY, notification)
    }

    companion object {
        private const val NOTIFICATION_ID_EMERGENCY = 9001
    }
}
