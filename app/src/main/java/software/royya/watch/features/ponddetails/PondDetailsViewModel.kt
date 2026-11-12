package software.royya.watch.features.ponddetails

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import software.royya.watch.domain.model.*
import software.royya.watch.domain.repository.AeratorRepository
import software.royya.watch.domain.repository.PondRepository
import javax.inject.Inject

data class PondDetailsUiState(
    val pondId: String = "pond_01",
    val pond: Pond? = null,
    val readings: SensorReading? = null,
    val aerator: AeratorStatus? = null,
    val selectedRange: String = "24h",
    val historyPoints: List<SensorReading> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class PondDetailsViewModel @Inject constructor(
    private val pondRepository: PondRepository,
    private val aeratorRepository: AeratorRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val pondId: String = savedStateHandle.get<String>("pondId") ?: "pond_01"

    private val _uiState = MutableStateFlow(PondDetailsUiState(pondId = pondId))
    val uiState: StateFlow<PondDetailsUiState> = _uiState.asStateFlow()

    init {
        loadPondDetails()
        loadHistory(_uiState.value.selectedRange)
    }

    private fun loadPondDetails() {
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

    fun selectRange(range: String) {
        _uiState.value = _uiState.value.copy(selectedRange = range)
        loadHistory(range)
    }

    private fun loadHistory(range: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val history = pondRepository.getSensorHistory(pondId, range)
            _uiState.value = _uiState.value.copy(historyPoints = history, isLoading = false)
        }
    }
}
