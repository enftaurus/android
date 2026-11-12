package software.royya.watch.features.onboarding

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import software.royya.watch.BuildConfig
import software.royya.watch.R
import software.royya.watch.core.design.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onOnboardingFinished: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val sampleAudioUri = "${BuildConfig.BACKEND_BASE_URL.trimEnd('/')}/api/voice/alert"

    Scaffold(
        containerColor = RoyyaBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Progress Dots
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(5) { step ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (step == uiState.currentStep) 24.dp else 8.dp, 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (step == uiState.currentStep) RoyyaPrimary
                                else if (step < uiState.currentStep) RoyyaSoftGreen
                                else RoyyaBorder
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step Content
            AnimatedContent(
                targetState = uiState.currentStep,
                label = "onboarding_step",
                modifier = Modifier.weight(1f)
            ) { step ->
                when (step) {
                    0 -> StepLanguage(
                        selectedLanguage = uiState.selectedLanguage,
                        onSelectLanguage = { viewModel.selectLanguage(it) }
                    )
                    1 -> StepWelcome()
                    2 -> StepProfile(
                        farmerName = uiState.farmerName,
                        onNameChange = { viewModel.updateFarmerName(it) }
                    )
                    3 -> StepPondPairing(
                        selectedPondId = uiState.selectedPondId,
                        onSelectPond = { viewModel.selectPond(it) }
                    )
                    4 -> StepVoiceTest(
                        isPlaying = uiState.isPlayingVoiceSample,
                        voiceError = uiState.voiceError,
                        onPlay = { viewModel.playVoiceTest(sampleAudioUri) },
                        onStop = { viewModel.stopVoiceTest() }
                    )
                }
            }

            // Bottom Navigation Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (uiState.currentStep > 0) {
                    OutlinedButton(
                        onClick = { viewModel.previousStep() },
                        modifier = Modifier
                            .defaultMinSize(minHeight = 52.dp)
                            .weight(1f)
                            .padding(end = 8.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("వెనక్కి (Back)", style = MaterialTheme.typography.labelLarge)
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                Button(
                    onClick = {
                        if (uiState.currentStep < 4) {
                            viewModel.nextStep()
                        } else {
                            viewModel.completeOnboarding(onOnboardingFinished)
                        }
                    },
                    modifier = Modifier
                        .defaultMinSize(minHeight = 52.dp)
                        .weight(1.4f),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyyaPrimary),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (uiState.currentStep == 4) stringResource(R.string.onboarding_btn_finish)
                        else "తరువాత (Next)",
                        style = MaterialTheme.typography.labelLarge,
                        color = RoyyaSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = RoyyaSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun StepLanguage(
    selectedLanguage: String,
    onSelectLanguage: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Translate,
            contentDescription = null,
            tint = RoyyaPrimary,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "మీ భాషను ఎంచుకోండి\nSelect Your Language",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "యాప్ స్క్రీన్‌లు మరియు ముఖ్యమైన వాయిస్ హెచ్చరికల కోసం ఉపయోగించబడుతుంది.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        // Telugu Option (Primary Recommendation)
        LanguageCard(
            title = "తెలుగు (Telugu)",
            subtitle = "సిఫార్సు చేయబడింది (Recommended for Farmers)",
            isSelected = selectedLanguage == "te",
            onClick = { onSelectLanguage("te") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // English Option
        LanguageCard(
            title = "English",
            subtitle = "Standard Technical English",
            isSelected = selectedLanguage == "en",
            onClick = { onSelectLanguage("en") }
        )
    }
}

@Composable
private fun LanguageCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) RoyyaSoftGreen else RoyyaSurface)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) RoyyaPrimary else RoyyaBorder,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) RoyyaPrimaryDark else RoyyaTextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) RoyyaPrimary else RoyyaTextSecondary
                )
            }
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = RoyyaPrimary)
            )
        }
    }
}

@Composable
private fun StepWelcome() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(RoyyaAccentLime),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.WaterDrop,
                contentDescription = null,
                tint = RoyyaPrimaryDark,
                modifier = Modifier.size(44.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.onboarding_welcome_title),
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.onboarding_welcome_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = RoyyaTextSecondary
        )
    }
}

@Composable
private fun StepProfile(
    farmerName: String,
    onNameChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.onboarding_profile_title),
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "హోమ్ స్క్రీన్‌లో మీకు స్వాగతం పలకడానికి మీ పేరును నమోదు చేయండి.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = farmerName,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.onboarding_profile_name)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun StepPondPairing(
    selectedPondId: String,
    onSelectPond: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.onboarding_pairing_title),
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.onboarding_pairing_subtitle),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(24.dp))

        listOf(
            "pond_01" to "Pond 01 (ఉత్తర చెరువు) · ESP32-ROYYA-01"
        ).forEach { (id, label) ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (selectedPondId == id) RoyyaSoftGreen else RoyyaSurface)
                    .border(
                        width = if (selectedPondId == id) 2.dp else 1.dp,
                        color = if (selectedPondId == id) RoyyaPrimary else RoyyaBorder,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onSelectPond(id) }
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (selectedPondId == id) FontWeight.Bold else FontWeight.Normal
                    )
                    if (selectedPondId == id) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = RoyyaPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepVoiceTest(
    isPlaying: Boolean,
    voiceError: String?,
    onPlay: () -> Unit,
    onStop: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.VolumeUp,
            contentDescription = null,
            tint = RoyyaPrimary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.onboarding_voice_test_title),
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.onboarding_voice_test_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { if (isPlaying) onStop() else onPlay() },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isPlaying) RoyyaCritical else RoyyaPrimary
            ),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.defaultMinSize(minWidth = 220.dp, minHeight = 56.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isPlaying) "ధ్వని ఆపండి (Stop)" else "ధ్వని వినండి (Test Audio)",
                style = MaterialTheme.typography.labelLarge
            )
        }
        if (voiceError != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = voiceError,
                style = MaterialTheme.typography.bodySmall,
                color = RoyyaCritical,
                textAlign = TextAlign.Center
            )
        }
    }
}
