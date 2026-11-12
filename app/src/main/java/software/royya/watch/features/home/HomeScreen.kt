package software.royya.watch.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import software.royya.watch.R
import software.royya.watch.core.design.*
import software.royya.watch.domain.model.*

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToAlertDetails: (String) -> Unit,
    onNavigateToPondDetails: (String) -> Unit,
    onOpenAeratorControls: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = RoyyaBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Greeting & Connectivity Status
            HeaderSection(
                farmerName = uiState.farmerName,
                pondName = uiState.pond?.name ?: "Pond 01",
                connectivity = uiState.connectivity
            )

            // Feedback Banner
            uiState.feedbackMessage?.let { msg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF92400E),
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.dismissFeedback() }) {
                            Text("OK", color = Color(0xFF92400E), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Overlay Permission Banner if not granted
            val context = androidx.compose.ui.platform.LocalContext.current
            val canDrawOverlays = android.provider.Settings.canDrawOverlays(context)
            if (!canDrawOverlays) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFFFEF3C7))
                        .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(18.dp))
                        .clickable {
                            val intent = android.content.Intent(
                                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                android.net.Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = Color(0xFFB45309)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "అత్యవసర కాల్ అలారం కోసం ఓవర్‌లే అనుమతి ఇవ్వండి",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFF78350F),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tap to allow 'Display over other apps' for incoming call alerts",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF92400E)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color(0xFFB45309)
                        )
                    }
                }
            }

            // Critical Alert Banner (if active)
            uiState.latestCriticalAlert?.let { alert ->
                LatestAlertCard(
                    alert = alert,
                    onClick = {
                        if (alert.audioReference?.contains("/api/voice/alert") == true) {
                            context.startActivity(
                                software.royya.watch.features.call.EmergencyCallActivity.createIntent(
                                    context = context,
                                    alertId = alert.id,
                                    title = alert.title,
                                    message = alert.message,
                                    dissolvedOxygen = uiState.readings?.dissolvedOxygen,
                                    audioUrl = alert.audioReference
                                )
                            )
                        } else {
                            onNavigateToAlertDetails(alert.id)
                        }
                    }
                )
            }

            // Pond Health Card
            PondHealthCard(
                pond = uiState.pond,
                dissolvedOxygen = uiState.readings?.dissolvedOxygen,
                onClick = { uiState.pond?.id?.let { onNavigateToPondDetails(it) } }
            )

            // Primary Metrics Grid Row 1: DO & Temperature
            val doVal = uiState.readings?.dissolvedOxygen
            val tempVal = uiState.readings?.temperature
            val turbidityVal = uiState.readings?.turbidity

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.metric_do),
                    value = doVal?.let { "%.2f".format(it) } ?: "--",
                    unit = stringResource(R.string.unit_mg_l),
                    subtitle = when (uiState.readings?.oxygenSource) {
                        OxygenSource.SIMULATED -> "Backend Override"
                        OxygenSource.PREDICTED -> "ML Model (${stringResource(R.string.oxygen_source_predicted)})"
                        else -> stringResource(R.string.oxygen_source_measured)
                    },
                    icon = Icons.Default.WaterDrop,
                    backgroundColor = MetricMint,
                    isWarning = doVal != null && doVal < 5.0f
                )

                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.metric_temp),
                    value = tempVal?.let { "%.2f".format(it) } ?: "--",
                    unit = stringResource(R.string.unit_celsius),
                    subtitle = if (tempVal == null || tempVal in 26f..32f) "సాధారణం (26–32°C)" else "పరిధి వెలుపల (Outside 26–32°C)",
                    icon = Icons.Default.Thermostat,
                    backgroundColor = MetricSand,
                    isWarning = tempVal != null && (tempVal > 32f || tempVal < 24f)
                )
            }

            // Primary Metrics Grid Row 2: Turbidity Voltage & Pond Details Shortcut
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.metric_turbidity),
                    value = turbidityVal?.let { "%.2f".format(it) } ?: "--",
                    unit = "V",
                    subtitle = if (turbidityVal != null && turbidityVal > 3.5f) {
                        "అధిక మడ్డితనం (> 3.5V)"
                    } else {
                        "సెన్సార్ వోల్టేజ్ (Sensor V)"
                    },
                    icon = Icons.Default.Opacity,
                    backgroundColor = Color(0xFFEFF6FF),
                    isWarning = turbidityVal != null && turbidityVal > 3.5f
                )

                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.aerator_title),
                    value = if (uiState.aerator?.state == AeratorRelayState.ON) "ON" else "OFF",
                    unit = uiState.aerator?.mode?.name ?: "AUTO",
                    subtitle = if (uiState.aerator?.state == AeratorRelayState.ON) "నడుస్తోంది (Running)" else "ఆపివేయబడింది (Stopped)",
                    icon = Icons.Default.Bolt,
                    backgroundColor = Color(0xFFF0FDF4),
                    isWarning = false
                )
            }

            // Aerator Status & Direct Control Card
            AeratorStatusCard(
                aerator = uiState.aerator,
                onToggleMode = { viewModel.toggleAeratorMode() },
                onToggleRelay = { viewModel.toggleManualRelay() },
                onOpenDetails = onOpenAeratorControls
            )

            // Last Updated & Connectivity Indicator
            LastUpdatedIndicator(
                lastUpdated = uiState.pond?.lastUpdated ?: "--"
            )
        }
    }
}

@Composable
private fun HeaderSection(
    farmerName: String,
    pondName: String,
    connectivity: Connectivity
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "${stringResource(R.string.greeting)}, $farmerName",
                style = MaterialTheme.typography.titleLarge,
                color = RoyyaTextPrimary
            )
            Text(
                text = pondName,
                style = MaterialTheme.typography.bodyMedium,
                color = RoyyaTextSecondary
            )
        }

        // Live / Offline Badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (connectivity == Connectivity.ONLINE) RoyyaSoftGreen else Color(0xFFFEE2E2))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (connectivity == Connectivity.ONLINE) RoyyaPrimary else RoyyaCritical)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (connectivity == Connectivity.ONLINE) stringResource(R.string.live_badge)
                    else stringResource(R.string.status_offline),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (connectivity == Connectivity.ONLINE) RoyyaPrimaryDark else RoyyaCritical
                )
            }
        }
    }
}

@Composable
private fun LatestAlertCard(
    alert: Alert,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFFDE8E8))
            .border(1.5.dp, RoyyaCritical, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(RoyyaCritical),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = RoyyaSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alert.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = RoyyaCritical
                )
                Text(
                    text = alert.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = RoyyaTextPrimary,
                    maxLines = 2
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = RoyyaCritical
            )
        }
    }
}

@Composable
private fun PondHealthCard(
    pond: Pond?,
    dissolvedOxygen: Float?,
    onClick: () -> Unit
) {
    val isOffline = pond?.connectivity == Connectivity.OFFLINE
    val score = pond?.healthScore
    val (statusLabel, statusColor, bgColor) = when {
        isOffline -> Triple(stringResource(R.string.status_offline), RoyyaCritical, Color(0xFFF3F4F6))
        score == null -> Triple("కనెక్ట్ అవుతోంది...", RoyyaTextSecondary, Color(0xFFF3F4F6))
        score >= 80 -> Triple(stringResource(R.string.pond_health_good), RoyyaPrimary, RoyyaSoftGreen)
        score >= 60 -> Triple(stringResource(R.string.pond_health_warning), RoyyaWarning, Color(0xFFFEF9E7))
        else -> Triple(stringResource(R.string.pond_health_critical), RoyyaCritical, Color(0xFFFDE8E8))
    }

    val bandNote = when {
        isOffline || dissolvedOxygen == null -> null
        dissolvedOxygen < 5.0f -> "తక్కువ ఆక్సిజన్ (< 5.0 mg/L) · ఆటో మోడ్‌లో ఏరేటర్ ఆన్"
        dissolvedOxygen <= 6.0f -> "రికవరీ / పర్యవేక్షణ జోన్ (5.0–6.0 mg/L)"
        else -> "ఆరోగ్యకరమైన ఆక్సిజన్ స్థాయి (> 6.0 mg/L)"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.pond_health).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = statusColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = score?.toString() ?: "--",
                    style = MaterialTheme.typography.displayLarge,
                    color = RoyyaPrimaryDark,
                    fontWeight = FontWeight.Black
                )
                if (score != null) {
                    Text(
                        text = "/100",
                        style = MaterialTheme.typography.headlineMedium,
                        color = RoyyaTextSecondary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = statusColor
            )
            if (bandNote != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = bandNote,
                    style = MaterialTheme.typography.labelSmall,
                    color = RoyyaTextSecondary
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    unit: String,
    subtitle: String,
    icon: ImageVector,
    backgroundColor: Color,
    isWarning: Boolean
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .border(
                width = if (isWarning) 1.5.dp else 1.dp,
                color = if (isWarning) RoyyaCritical else RoyyaBorder,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(16.dp)
    ) {
        Column {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isWarning) RoyyaCritical else RoyyaPrimaryDark,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = RoyyaTextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isWarning) RoyyaCritical else RoyyaTextPrimary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelMedium,
                    color = RoyyaTextSecondary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (isWarning) RoyyaCritical else RoyyaPrimary
            )
        }
    }
}

@Composable
private fun AeratorStatusCard(
    aerator: AeratorStatus?,
    onToggleMode: () -> Unit,
    onToggleRelay: () -> Unit,
    onOpenDetails: () -> Unit
) {
    val isRunning = aerator?.state == AeratorRelayState.ON
    val isAuto = aerator?.mode == AeratorMode.AUTO

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(RoyyaPrimaryDark)
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Power,
                            contentDescription = null,
                            tint = if (isRunning) RoyyaAccentLime else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.aerator_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = RoyyaSurface
                        )
                        Text(
                            text = if (isRunning) stringResource(R.string.aerator_running)
                            else stringResource(R.string.aerator_stopped),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                }

                // Mode Badge Button (AUTO / MANUAL)
                FilledTonalButton(
                    onClick = onToggleMode,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isAuto) RoyyaSoftGreen else Color.White.copy(alpha = 0.2f),
                        contentColor = if (isAuto) RoyyaPrimaryDark else RoyyaSurface
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isAuto) "ఆటో (AUTO)" else "మాన్యువల్ (MANUAL)",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action / Hysteresis Info
            if (isAuto) {
                Text(
                    text = stringResource(R.string.aerator_auto_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "మాన్యువల్ నియంత్రణ (Manual Override)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Button(
                        onClick = onToggleRelay,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRunning) Color(0xFFEF4444) else RoyyaAccentLime,
                            contentColor = if (isRunning) Color.White else RoyyaPrimaryDark
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = if (isRunning) stringResource(R.string.aerator_turn_off)
                            else stringResource(R.string.aerator_turn_on),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LastUpdatedIndicator(
    lastUpdated: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.last_updated, lastUpdated),
            style = MaterialTheme.typography.labelMedium,
            color = RoyyaTextSecondary
        )
    }
}
