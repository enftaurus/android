package software.royya.watch.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import software.royya.watch.domain.model.*
import software.royya.watch.domain.repository.*
import javax.inject.Inject

data class HomeUiState(
    val farmerName: String = "Ravi Kumar",
    val selectedPondId: String = "pond_01",
    val pond: Pond? = null,
    val readings: SensorReading? = null,
    val aerator: AeratorStatus? = null,
    val latestCriticalAlert: Alert? = null,
    val connectivity: Connectivity = Connectivity.ONLINE,
    val isLoading: Boolean = false,
    val feedbackMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val pondRepository: PondRepository,
    private val aeratorRepository: AeratorRepository,
    private val alertRepository: AlertRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            preferencesRepository.getFarmerName().collect { name ->
                _uiState.value = _uiState.value.copy(farmerName = name)
            }
        }

        viewModelScope.launch {
            preferencesRepository.getSelectedPondId().collect { pondId ->
                _uiState.value = _uiState.value.copy(selectedPondId = pondId)
                observePondData(pondId)
            }
        }

        viewModelScope.launch {
            pondRepository.observeConnectivity().collect { conn ->
                _uiState.value = _uiState.value.copy(connectivity = conn)
            }
        }

        viewModelScope.launch {
            alertRepository.observeAlerts().collect { alerts ->
                val critical = alerts.firstOrNull { it.severity == AlertSeverity.CRITICAL && it.status != AlertStatus.RESOLVED }
                _uiState.value = _uiState.value.copy(latestCriticalAlert = critical)
            }
        }
    }

    private fun observePondData(pondId: String) {
        viewModelScope.launch {
            pondRepository.observePondSummary(pondId).collect { pond ->
                _uiState.value = _uiState.value.copy(pond = pond)
            }
        }

        viewModelScope.launch {
            pondRepository.getLatestReadings(pondId).collect { readings ->
                _uiState.value = _uiState.value.copy(readings = readings)
            }
        }

        viewModelScope.launch {
            aeratorRepository.getState(pondId).collect { aerator ->
                _uiState.value = _uiState.value.copy(aerator = aerator)
            }
        }
    }

    fun toggleAeratorMode() {
        val current = _uiState.value.aerator ?: return
        val nextMode = if (current.mode == AeratorMode.AUTO) AeratorMode.MANUAL else AeratorMode.AUTO
        viewModelScope.launch {
            val result = aeratorRepository.setMode(current.pondId, nextMode)
            result.fold(
                onSuccess = { _uiState.value = _uiState.value.copy(aerator = it) },
                onFailure = { _uiState.value = _uiState.value.copy(feedbackMessage = it.message) }
            )
        }
    }

    fun toggleManualRelay() {
        val current = _uiState.value.aerator ?: return
        if (current.mode == AeratorMode.AUTO) {
            _uiState.value = _uiState.value.copy(feedbackMessage = "Cannot toggle relay manually in AUTO mode")
            return
        }
        val nextState = if (current.state == AeratorRelayState.ON) AeratorRelayState.OFF else AeratorRelayState.ON
        viewModelScope.launch {
            val result = aeratorRepository.setManualState(current.pondId, nextState)
            result.fold(
                onSuccess = { _uiState.value = _uiState.value.copy(aerator = it) },
                onFailure = { _uiState.value = _uiState.value.copy(feedbackMessage = it.message) }
            )
        }
    }

    fun dismissFeedback() {
        _uiState.value = _uiState.value.copy(feedbackMessage = null)
    }
}
