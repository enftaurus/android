package software.royya.watch.features.call

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dagger.hilt.android.AndroidEntryPoint
import software.royya.watch.audio.player.PlaybackState
import software.royya.watch.core.design.RoyyaAccentLime
import software.royya.watch.core.design.RoyyaCritical
import software.royya.watch.core.design.RoyyaPrimaryDark
import software.royya.watch.core.design.RoyyaWatchTheme

@AndroidEntryPoint
class EmergencyCallActivity : ComponentActivity() {

    private val viewModel: EmergencyCallViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureLockScreenOverlay()
        if (savedInstanceState == null) {
            handleIntentExtras(intent)
        }

        setContent {
            RoyyaWatchTheme {
                val uiState by viewModel.uiState.collectAsState()

                LaunchedEffect(uiState.shouldDismiss) {
                    if (uiState.shouldDismiss) {
                        finish()
                    }
                }

                EmergencyCallScreen(
                    uiState = uiState,
                    onAnswer = { viewModel.answerCall() },
                    onPressKey = { key -> viewModel.pressKeypad(key) },
                    onDismiss = { viewModel.dismissCall() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntentExtras(intent)
    }

    private fun handleIntentExtras(intent: Intent?) {
        val alertId = intent?.getStringExtra(EXTRA_ALERT_ID) ?: "active"
        val title = intent?.getStringExtra(EXTRA_TITLE) ?: "అత్యవసర హెచ్చరిక: తక్కువ ఆక్సిజన్"
        val message = intent?.getStringExtra(EXTRA_MESSAGE)
            ?: "నమస్కారం! రొయ్య వాచ్ నుండి అత్యవసర హెచ్చరిక. మీ రొయ్యల చెరువులో ఆక్సిజన్ స్థాయి ప్రమాదకరంగా తగ్గిపోయింది. వెంటనే ఏరేటర్ ఆన్ చేయడానికి 1 నొక్కండి, ఏరేటర్ ఆఫ్ చేయడానికి 2 నొక్కండి."
        val doValue = if (intent?.hasExtra(EXTRA_DO_VALUE) == true) {
            intent.getFloatExtra(EXTRA_DO_VALUE, -1f).takeIf { it >= 0f }
        } else {
            null
        }
        val audioUrl = intent?.getStringExtra(EXTRA_AUDIO_URL)

        viewModel.initializeAlert(
            alertId = alertId,
            title = title,
            message = message,
            dissolvedOxygen = doValue,
            audioUrl = audioUrl
        )
    }

    private fun configureLockScreenOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onDestroy() {
        viewModel.stopAudioOnExit()
        if (isFinishing) {
            androidx.core.app.NotificationManagerCompat.from(this).cancel(9001)
        }
        super.onDestroy()
    }

    companion object {
        const val EXTRA_ALERT_ID = "extra_alert_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_DO_VALUE = "extra_do_value"
        const val EXTRA_AUDIO_URL = "extra_audio_url"

        fun createIntent(
            context: Context,
            alertId: String,
            title: String,
            message: String,
            dissolvedOxygen: Float?,
            audioUrl: String?
        ): Intent {
            return Intent(context, EmergencyCallActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                )
                putExtra(EXTRA_ALERT_ID, alertId)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_MESSAGE, message)
                if (dissolvedOxygen != null) {
                    putExtra(EXTRA_DO_VALUE, dissolvedOxygen)
                }
                if (!audioUrl.isNullOrBlank()) {
                    putExtra(EXTRA_AUDIO_URL, audioUrl)
                }
            }
        }
    }
}

@Composable
private fun EmergencyCallScreen(
    uiState: EmergencyCallUiState,
    onAnswer: () -> Unit,
    onPressKey: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val isRinging = uiState.callStage == CallStage.RINGING
    val isSpeaking = !isRinging && (
        uiState.playbackState is PlaybackState.Playing ||
            uiState.playbackState is PlaybackState.Loading
        )
    val scrollState = rememberScrollState()

    val infiniteTransition = rememberInfiniteTransition(label = "call_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = if (isRinging || isSpeaking) 1.14f else 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isRinging) 550 else 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "call_pulse_scale"
    )

    Scaffold(
        containerColor = Color(0xFF0E1715)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 22.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Incoming Emergency Call Header
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(RoyyaCritical.copy(alpha = 0.22f))
                        .border(1.dp, RoyyaCritical, RoundedCornerShape(50))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(RoyyaCritical)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isRinging) {
                                "రొయ్య వాచ్ • అత్యవసర కాల్ వస్తోంది…"
                            } else {
                                "రొయ్య వాచ్ • లైవ్ వాయిస్ కాల్"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Pulsing Call Avatar
                Box(
                    modifier = Modifier
                        .size(124.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    (if (isRinging) Color(0xFF16A34A) else RoyyaCritical).copy(alpha = 0.92f),
                                    (if (isRinging) Color(0xFF16A34A) else RoyyaCritical).copy(alpha = 0.2f)
                                )
                            )
                        )
                        .border(3.dp, if (isRinging) RoyyaAccentLime else RoyyaCritical, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSpeaking) Icons.Default.VolumeUp else Icons.Default.PhoneInTalk,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(54.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Live Dissolved Oxygen Pill
                uiState.dissolvedOxygen?.let { doVal ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF2B1414))
                            .border(1.dp, RoyyaCritical.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "ఆక్సిజన్ స్థాయి: %.2f mg/L".format(doVal),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFCA5A5)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Text(
                    text = uiState.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (isRinging) {
                    Text(
                        text = "అలారం రింగ్ అవుతోంది…\nమాట్లాడటానికి కాల్ ఎత్తండి (Answer Call)",
                        style = MaterialTheme.typography.titleMedium,
                        color = RoyyaAccentLime,
                        textAlign = TextAlign.Center,
                        lineHeight = 26.sp
                    )
                } else {
                    Text(
                        text = uiState.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Automatic Live Speech Status Badge (no manual play button)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (isSpeaking) RoyyaAccentLime.copy(alpha = 0.16f)
                                else Color.White.copy(alpha = 0.10f)
                            )
                            .border(
                                1.dp,
                                if (isSpeaking) RoyyaAccentLime else Color.White.copy(alpha = 0.25f),
                                RoundedCornerShape(50)
                            )
                            .padding(horizontal = 18.dp, vertical = 9.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = if (isSpeaking) RoyyaAccentLime else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSpeaking) {
                                    "తెలుగు వాయిస్ వినిపిస్తోంది…"
                                } else {
                                    "1 లేదా 2 నొక్కండి (Press 1 or 2)"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSpeaking) RoyyaAccentLime else Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (isRinging) {
                // Incoming Call Actions: Answer to hear voice automatically, or Decline
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Button(
                        onClick = onAnswer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF16A34A),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "కాల్ ఎత్తండి (Answer Call)",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Button(
                        onClick = onDismiss,
                        enabled = !uiState.isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RoyyaCritical,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "తిరస్కరించండి (Decline)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                // Confirmation feedback if user pressed 1 or 2
                uiState.actionFeedback?.let { feedback ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF153B35))
                            .border(1.5.dp, RoyyaAccentLime, RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = feedback,
                            style = MaterialTheme.typography.titleMedium,
                            color = RoyyaAccentLime,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // IVR Call Keypad: Press 1 for Aerator ON, Press 2 for Aerator OFF
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "ఎంపిక కోసం నొక్కండి (Press Keypad Option)",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    KeypadOptionCard(
                        digit = "1",
                        teluguLabel = "ఏరేటర్ ఆన్ చేయండి",
                        englishLabel = "Press 1 · Turn Aerator ON",
                        accentColor = RoyyaAccentLime,
                        contentColor = RoyyaPrimaryDark,
                        containerColor = Color(0xFF163D2B),
                        icon = Icons.Default.Power,
                        isSelected = uiState.selectedKey == "1",
                        enabled = !uiState.isSubmitting,
                        onClick = { onPressKey("1") }
                    )

                    KeypadOptionCard(
                        digit = "2",
                        teluguLabel = "ఏరేటర్ ఆఫ్ చేయండి",
                        englishLabel = "Press 2 · Turn Aerator OFF",
                        accentColor = Color(0xFFFCA5A5),
                        contentColor = Color.White,
                        containerColor = Color(0xFF3B1919),
                        icon = Icons.Default.PowerOff,
                        isSelected = uiState.selectedKey == "2",
                        enabled = !uiState.isSubmitting,
                        onClick = { onPressKey("2") }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // End Call / Acknowledge
                    Button(
                        onClick = onDismiss,
                        enabled = !uiState.isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RoyyaCritical,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        if (uiState.isSubmitting) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "కాల్ ముగించండి / అర్థమైంది",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadOptionCard(
    digit: String,
    teluguLabel: String,
    englishLabel: String,
    accentColor: Color,
    contentColor: Color,
    containerColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(containerColor)
            .border(
                width = if (isSelected) 2.5.dp else 1.5.dp,
                color = if (isSelected) accentColor else accentColor.copy(alpha = 0.55f),
                shape = RoundedCornerShape(22.dp)
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Big dialpad digit circle
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(accentColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = digit,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = if (digit == "1") RoyyaPrimaryDark else Color(0xFF450A0A)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = teluguLabel,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = englishLabel,
                style = MaterialTheme.typography.bodySmall,
                color = accentColor
            )
        }

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(28.dp)
        )
    }
}
