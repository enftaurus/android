package software.royya.watch.features.alertdetails

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import software.royya.watch.audio.player.AudioPlayerManager
import software.royya.watch.audio.player.PlaybackState
import software.royya.watch.domain.model.Alert
import software.royya.watch.domain.model.AlertStatus
import software.royya.watch.domain.repository.AlertRepository
import software.royya.watch.domain.repository.AudioRepository
import javax.inject.Inject

data class AlertDetailsUiState(
    val alert: Alert? = null,
    val playbackState: PlaybackState = PlaybackState.Idle,
    val isAcknowledged: Boolean = false,
    val isLoading: Boolean = false
)

@HiltViewModel
class AlertDetailsViewModel @Inject constructor(
    private val alertRepository: AlertRepository,
    private val audioRepository: AudioRepository,
    private val audioPlayerManager: AudioPlayerManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val alertId: String = savedStateHandle.get<String>("alertId") ?: "alert_1024"

    private val _uiState = MutableStateFlow(AlertDetailsUiState())
    val uiState: StateFlow<AlertDetailsUiState> = _uiState.asStateFlow()

    init {
        loadAlert()
        observePlayback()
    }

    private fun loadAlert() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val alert = alertRepository.getAlert(alertId)
            _uiState.value = _uiState.value.copy(
                alert = alert,
                isAcknowledged = alert?.status == AlertStatus.ACKNOWLEDGED,
                isLoading = false
            )
        }
    }

    private fun observePlayback() {
        viewModelScope.launch {
            audioPlayerManager.playbackState.collect { state ->
                _uiState.value = _uiState.value.copy(playbackState = state)
            }
        }
    }

    fun playAudio() {
        val alert = _uiState.value.alert ?: return
        viewModelScope.launch {
            val audioUriResult = audioRepository.getAudioUri(alert.id, alert.audioReference)
            val uri = audioUriResult.getOrDefault("")
            audioPlayerManager.play(uri, useAlarmStream = false)
        }
    }

    fun pauseAudio() {
        audioPlayerManager.pause()
    }

    fun stopAudio() {
        audioPlayerManager.stop()
    }

    fun acknowledgeAlert(onDone: () -> Unit = {}) {
        val alert = _uiState.value.alert ?: return
        viewModelScope.launch {
            audioPlayerManager.stop()
            alertRepository.acknowledgeAlert(alert.id)
            _uiState.value = _uiState.value.copy(isAcknowledged = true)
            onDone()
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayerManager.stop()
    }
}
