package software.royya.watch.features.alerts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import software.royya.watch.R
import software.royya.watch.core.design.*
import software.royya.watch.domain.model.Alert
import software.royya.watch.domain.model.AlertSeverity
import software.royya.watch.domain.model.AlertStatus

@Composable
fun AlertsScreen(
    viewModel: AlertsViewModel,
    onNavigateToAlertDetails: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val filteredAlerts = remember(uiState.alerts, uiState.selectedFilter) {
        when (uiState.selectedFilter) {
            "CRITICAL" -> uiState.alerts.filter { it.severity == AlertSeverity.CRITICAL }
            "WARNING" -> uiState.alerts.filter { it.severity == AlertSeverity.WARNING }
            "RESOLVED" -> uiState.alerts.filter { it.status == AlertStatus.RESOLVED }
            else -> uiState.alerts
        }
    }

    Scaffold(
        containerColor = RoyyaBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
        ) {
            Text(
                text = stringResource(R.string.alerts_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Filter Chips
            FilterRow(
                selectedFilter = uiState.selectedFilter,
                onSelectFilter = { viewModel.setFilter(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Alert List or Empty State
            if (filteredAlerts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = RoyyaPrimary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.no_active_alerts),
                            style = MaterialTheme.typography.bodyLarge,
                            color = RoyyaTextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredAlerts, key = { it.id }) { alert ->
                        AlertItemCard(
                            alert = alert,
                            isPlaying = uiState.currentlyPlayingAlertId == alert.id,
                            onClick = { onNavigateToAlertDetails(alert.id) },
                            onReplayAudio = { viewModel.replayAudio(alert) },
                            onStopAudio = { viewModel.stopAudio() },
                            onAcknowledge = { viewModel.acknowledge(alert.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterRow(
    selectedFilter: String,
    onSelectFilter: (String) -> Unit
) {
    val filters = listOf(
        "ALL" to stringResource(R.string.filter_all),
        "CRITICAL" to stringResource(R.string.filter_critical),
        "WARNING" to stringResource(R.string.filter_warning),
        "RESOLVED" to stringResource(R.string.filter_resolved)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { (key, label) ->
            val isSelected = selectedFilter == key
            FilterChip(
                selected = isSelected,
                onClick = { onSelectFilter(key) },
                label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = RoyyaPrimary,
                    selectedLabelColor = RoyyaSurface
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
private fun AlertItemCard(
    alert: Alert,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onReplayAudio: () -> Unit,
    onStopAudio: () -> Unit,
    onAcknowledge: () -> Unit
) {
    val (statusColor, badgeBg) = when (alert.severity) {
        AlertSeverity.CRITICAL -> RoyyaCritical to Color(0xFFFDE8E8)
        AlertSeverity.WARNING -> RoyyaWarning to Color(0xFFFEF9E7)
        AlertSeverity.INFO -> RoyyaPrimary to RoyyaSoftGreen
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(RoyyaSurface)
            .border(1.dp, RoyyaBorder, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(18.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Severity Tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = alert.severity.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor
                    )
                }

                // Timestamp
                Text(
                    text = alert.createdAt.take(16).replace("T", " "),
                    style = MaterialTheme.typography.labelSmall,
                    color = RoyyaTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title & Message
            Text(
                text = alert.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = RoyyaTextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = alert.message,
                style = MaterialTheme.typography.bodyMedium,
                color = RoyyaTextSecondary
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons Row: Voice Replay & Acknowledge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Audio Replay button
                if (alert.audioReference != null) {
                    FilledTonalButton(
                        onClick = { if (isPlaying) onStopAudio() else onReplayAudio() },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isPlaying) RoyyaCritical else RoyyaSoftGreen,
                            contentColor = if (isPlaying) RoyyaSurface else RoyyaPrimaryDark
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.VolumeUp,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPlaying) "ఆపండి" else stringResource(R.string.action_replay_audio),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                if (alert.status == AlertStatus.NEW) {
                    TextButton(onClick = onAcknowledge) {
                        Text(
                            text = stringResource(R.string.alert_btn_acknowledge),
                            style = MaterialTheme.typography.labelLarge,
                            color = RoyyaPrimary
                        )
                    }
                } else {
                    Text(
                        text = if (alert.status == AlertStatus.RESOLVED) stringResource(R.string.alert_resolved)
                        else stringResource(R.string.alert_acknowledged),
                        style = MaterialTheme.typography.labelSmall,
                        color = RoyyaPrimary
                    )
                }
            }
        }
    }
}
