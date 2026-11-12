package software.royya.watch

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import software.royya.watch.notifications.channels.NotificationChannelManager
import software.royya.watch.notifications.handlers.EmergencyCallLauncher
import javax.inject.Inject

@HiltAndroidApp
class RoyyaApplication : Application() {

    @Inject
    lateinit var notificationChannelManager: NotificationChannelManager

    @Inject
    lateinit var emergencyCallLauncher: EmergencyCallLauncher

    override fun onCreate() {
        super.onCreate()
        notificationChannelManager.createNotificationChannels()
        emergencyCallLauncher.startMonitoring()
    }
}
