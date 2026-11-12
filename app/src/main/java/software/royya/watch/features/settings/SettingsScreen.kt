package software.royya.watch.features.settings

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import software.royya.watch.BuildConfig
import software.royya.watch.R
import software.royya.watch.core.design.*

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val sampleAudioUri = "${BuildConfig.BACKEND_BASE_URL.trimEnd('/')}/api/voice/alert"

    Scaffold(
        containerColor = RoyyaBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )

            // Section: Farmer Profile
            SettingsCard(title = stringResource(R.string.settings_section_farmer)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(RoyyaSoftGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = RoyyaPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = uiState.farmerName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "భీమవరం క్లస్టర్ A (Bhimavaram Cluster A)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = RoyyaTextSecondary
                        )
                    }
                }
            }

            // Section: Language Selection
            SettingsCard(title = "భాష (Language)") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LanguageRow(
                        title = "తెలుగు (Telugu)",
                        isSelected = uiState.selectedLanguage == "te",
                        onClick = { viewModel.setLanguage("te") }
                    )
                    LanguageRow(
                        title = "English",
                        isSelected = uiState.selectedLanguage == "en",
                        onClick = { viewModel.setLanguage("en") }
                    )
                }
            }

            // Section: Voice Alerts & Audio Controls
            SettingsCard(title = stringResource(R.string.settings_section_audio)) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_voice_alerts_enabled),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "ఆక్సిజన్ పడిపోయినప్పుడు బిగ్గరగా తెలుగు వాయిస్ హెచ్చరికలను వినండి",
                                style = MaterialTheme.typography.labelSmall,
                                color = RoyyaTextSecondary
                            )
                        }
                        Switch(
                            checked = uiState.voiceAlertsEnabled,
                            onCheckedChange = { viewModel.setVoiceAlerts(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = RoyyaPrimary, checkedTrackColor = RoyyaSoftGreen)
                        )
                    }

                    HorizontalDivider(color = RoyyaBorder)

                    // Audio Test Button
                    FilledTonalButton(
                        onClick = {
                            if (uiState.isPlayingSample) viewModel.stopSampleVoice()
                            else viewModel.playSampleVoice(sampleAudioUri)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = if (uiState.isPlayingSample) Icons.Default.Stop else Icons.Default.VolumeUp,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.isPlayingSample) "వాయిస్ ఆపండి (Stop Voice)"
                            else stringResource(R.string.settings_test_voice)
                        )
                    }

                    HorizontalDivider(color = RoyyaBorder)

                    // Overlay Permission Row
                    val canOverlay = android.provider.Settings.canDrawOverlays(context)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "అత్యవసర కాల్ ఓవర్‌లే అనుమతి (Overlay)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (canOverlay) "అనుమతి ఇవ్వబడింది (Granted)" else "అనుమతి అవసరం (Required for call alarm)",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (canOverlay) RoyyaPrimary else RoyyaCritical
                            )
                        }
                        if (!canOverlay) {
                            TextButton(
                                onClick = {
                                    val intent = android.content.Intent(
                                        android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        android.net.Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                }
                            ) {
                                Text("Allow", color = RoyyaPrimary, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = RoyyaPrimary
                            )
                        }
                    }
                }
            }

            // Section: App Information & Data Source
            SettingsCard(title = stringResource(R.string.settings_section_app)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.settings_version),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = stringResource(R.string.settings_data_source, BuildConfig.DATA_SOURCE),
                        style = MaterialTheme.typography.bodyMedium,
                        color = RoyyaPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Backend Base URL: ${BuildConfig.BACKEND_BASE_URL}",
                        style = MaterialTheme.typography.labelSmall,
                        color = RoyyaTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = RoyyaTextSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(RoyyaSurface)
                .border(1.dp, RoyyaBorder, RoundedCornerShape(20.dp))
                .padding(18.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun LanguageRow(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) RoyyaSoftGreen else RoyyaBackground)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) RoyyaPrimaryDark else RoyyaTextPrimary
        )
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = RoyyaPrimary)
        )
    }
}
