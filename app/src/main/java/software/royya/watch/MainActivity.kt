package software.royya.watch

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import software.royya.watch.core.design.RoyyaPrimary
import software.royya.watch.core.design.RoyyaWatchTheme
import software.royya.watch.domain.repository.PreferencesRepository
import software.royya.watch.navigation.RoyyaNavGraph
import software.royya.watch.navigation.Screen
import software.royya.watch.notifications.handlers.EmergencyCallLauncher
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    @Inject
    lateinit var emergencyCallLauncher: EmergencyCallLauncher

    private var showOverlayPermissionDialog by mutableStateOf(false)
    private var overlayPromptDismissedThisSession = false

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        checkOverlayPermission()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        emergencyCallLauncher.startMonitoring()
        requestRequiredPermissions()

        setContent {
            RoyyaWatchTheme {
                val navController = rememberNavController()
                val isOnboardingCompleted by preferencesRepository.isOnboardingCompleted()
                    .collectAsState(initial = false)

                val startDestination = if (isOnboardingCompleted) {
                    Screen.Home.route
                } else {
                    Screen.Onboarding.route
                }

                RoyyaNavGraph(
                    navController = navController,
                    startDestination = startDestination
                )

                if (showOverlayPermissionDialog) {
                    AlertDialog(
                        onDismissRequest = {
                            overlayPromptDismissedThisSession = true
                            showOverlayPermissionDialog = false
                        },
                        title = {
                            Text(
                                text = "అత్యవసర కాల్ ఓవర్‌లే అనుమతి",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Text(
                                text = "ఆక్సిజన్ తగ్గినప్పుడు ఇతర యాప్‌ల పైన కూడా అత్యవసర తెలుగు వాయిస్ కాల్ అలారం కనిపించడానికి 'Display over other apps' అనుమతి ఇవ్వండి.\n\nGrant 'Display over other apps' permission so emergency voice call alarms appear immediately over any screen."
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showOverlayPermissionDialog = false
                                    openOverlaySettings()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RoyyaPrimary)
                            ) {
                                Text("అనుమతి ఇవ్వండి (Allow)")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    overlayPromptDismissedThisSession = true
                                    showOverlayPermissionDialog = false
                                }
                            ) {
                                Text("తర్వాత (Later)")
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (Settings.canDrawOverlays(this)) {
            showOverlayPermissionDialog = false
        }
    }

    private fun requestRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        checkOverlayPermission()
    }

    private fun checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this) && !overlayPromptDismissedThisSession) {
            showOverlayPermissionDialog = true
        }
    }

    private fun openOverlaySettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }
}
