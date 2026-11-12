package software.royya.watch.features.call

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import software.royya.watch.BuildConfig
import software.royya.watch.audio.player.AudioPlayerManager
import software.royya.watch.audio.player.PlaybackState
import software.royya.watch.data.remote.RoyyaApiService
import software.royya.watch.data.remote.dto.KeypadRequestDto
import software.royya.watch.data.repository.RemotePondRepository
import software.royya.watch.domain.repository.AlertRepository
import javax.inject.Inject

enum class CallStage {
    RINGING,
    ANSWERED
}

data class EmergencyCallUiState(
    val callStage: CallStage = CallStage.RINGING,
    val alertId: String = "",
    val title: String = "అత్యవసర హెచ్చరిక: తక్కువ ఆక్సిజన్",
    val message: String = "నమస్కారం! రొయ్య వాచ్ నుండి అత్యవసర హెచ్చరిక. మీ రొయ్యల చెరువులో ఆక్సిజన్ స్థాయి ప్రమాదకరంగా తగ్గిపోయింది. వెంటనే ఏరేటర్ ఆన్ చేయడానికి 1 నొక్కండి, ఏరేటర్ ఆఫ్ చేయడానికి 2 నొక్కండి.",
    val dissolvedOxygen: Float? = null,
    val audioUrl: String = "${BuildConfig.BACKEND_BASE_URL.trimEnd('/')}/api/voice/alert",
    val playbackState: PlaybackState = PlaybackState.Idle,
    val isSubmitting: Boolean = false,
    val selectedKey: String? = null,
    val actionFeedback: String? = null,
    val shouldDismiss: Boolean = false
)

@HiltViewModel
class EmergencyCallViewModel @Inject constructor(
    private val apiService: RoyyaApiService,
    private val audioPlayerManager: AudioPlayerManager,
    private val callRinger: EmergencyCallRinger,
    private val alertRepository: AlertRepository,
    private val remotePondRepository: RemotePondRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmergencyCallUiState())
    val uiState: StateFlow<EmergencyCallUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            audioPlayerManager.playbackState.collect { state ->
                _uiState.value = _uiState.value.copy(playbackState = state)
            }
        }
    }

    fun initializeAlert(
        alertId: String,
        title: String,
        message: String,
        dissolvedOxygen: Float?,
        audioUrl: String?
    ) {
        val baseUrl = BuildConfig.BACKEND_BASE_URL.trimEnd('/')
        val resolvedAudioUrl = when {
            audioUrl.isNullOrBlank() -> "$baseUrl/api/voice/alert"
            audioUrl.startsWith("/") -> "$baseUrl$audioUrl"
            else -> audioUrl
        }
        audioPlayerManager.stop()
        _uiState.value = EmergencyCallUiState(
            callStage = CallStage.RINGING,
            alertId = alertId,
            title = title,
            message = message,
            dissolvedOxygen = dissolvedOxygen,
            audioUrl = resolvedAudioUrl
        )
        callRinger.startRinging()
    }

    fun answerCall() {
        if (_uiState.value.callStage == CallStage.ANSWERED) return
        callRinger.stopRinging()
        _uiState.value = _uiState.value.copy(callStage = CallStage.ANSWERED)
        audioPlayerManager.play(_uiState.value.audioUrl, useAlarmStream = true)
    }

    fun pressKeypad(key: String) {
        if (_uiState.value.isSubmitting) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, selectedKey = key)
            callRinger.stopRinging()
            audioPlayerManager.stop()

            var feedbackText = if (key == "1") {
                "కీ 1: ఏరేటర్ ఆన్ చేయబడింది (Aerator ON · MANUAL)"
            } else {
                "కీ 2: ఏరేటర్ ఆఫ్ చేయబడింది (Aerator OFF · MANUAL)"
            }

            try {
                val resp = apiService.keypadInput(KeypadRequestDto(key))
                if (!resp.isSuccessful) {
                    feedbackText = "సర్వర్ లోపం (${resp.code()}) — మళ్ళీ ప్రయత్నించండి"
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        actionFeedback = feedbackText
                    )
                    return@launch
                }
                alertRepository.acknowledgeAlert(_uiState.value.alertId)
                remotePondRepository.refreshNow()
            } catch (e: Exception) {
                feedbackText = "నెట్‌వర్క్ లోపం: ${e.message ?: "కనెక్షన్ లేదు"}"
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    actionFeedback = feedbackText
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                isSubmitting = false,
                actionFeedback = feedbackText
            )
            delay(900L)
            _uiState.value = _uiState.value.copy(shouldDismiss = true)
        }
    }

    fun dismissCall() {
        if (_uiState.value.isSubmitting) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true)
            callRinger.stopRinging()
            audioPlayerManager.stop()
            alertRepository.acknowledgeAlert(_uiState.value.alertId)
            _uiState.value = _uiState.value.copy(isSubmitting = false, shouldDismiss = true)
        }
    }

    fun stopAudioOnExit() {
        callRinger.stopRinging()
        audioPlayerManager.stop()
    }

    override fun onCleared() {
        stopAudioOnExit()
        super.onCleared()
    }
}
