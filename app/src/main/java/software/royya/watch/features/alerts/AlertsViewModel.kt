package software.royya.watch.features.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import software.royya.watch.audio.player.AudioPlayerManager
import software.royya.watch.audio.player.PlaybackState
import software.royya.watch.domain.model.Alert
import software.royya.watch.domain.model.AlertSeverity
import software.royya.watch.domain.model.AlertStatus
import software.royya.watch.domain.repository.AlertRepository
import software.royya.watch.domain.repository.AudioRepository
import javax.inject.Inject

data class AlertsUiState(
    val alerts: List<Alert> = emptyList(),
    val selectedFilter: String = "ALL",
    val currentlyPlayingAlertId: String? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val alertRepository: AlertRepository,
    private val audioRepository: AudioRepository,
    private val audioPlayerManager: AudioPlayerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlertsUiState())
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    init {
        loadAlerts()
        observePlayback()
    }

    private fun observePlayback() {
        viewModelScope.launch {
            audioPlayerManager.playbackState.collect { state ->
                if (state is PlaybackState.Idle || state is PlaybackState.Completed || state is PlaybackState.Paused || state is PlaybackState.Error) {
                    _uiState.value = _uiState.value.copy(currentlyPlayingAlertId = null)
                }
            }
        }
    }

    private fun loadAlerts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            alertRepository.observeAlerts().collect { list ->
                _uiState.value = _uiState.value.copy(alerts = list, isLoading = false)
            }
        }
    }

    fun setFilter(filter: String) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
    }

    fun replayAudio(alert: Alert) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(currentlyPlayingAlertId = alert.id)
            val uriResult = audioRepository.getAudioUri(alert.id, alert.audioReference)
            val uri = uriResult.getOrDefault("")
            audioPlayerManager.play(uri, useAlarmStream = false)
        }
    }

    fun stopAudio() {
        audioPlayerManager.stop()
        _uiState.value = _uiState.value.copy(currentlyPlayingAlertId = null)
    }

    fun acknowledge(alertId: String) {
        viewModelScope.launch {
            alertRepository.acknowledgeAlert(alertId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayerManager.stop()
    }
}
