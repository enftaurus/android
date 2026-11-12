package software.royya.watch.features.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import software.royya.watch.audio.player.AudioPlayerManager
import software.royya.watch.audio.player.PlaybackState
import software.royya.watch.domain.repository.PreferencesRepository
import javax.inject.Inject

data class OnboardingUiState(
    val currentStep: Int = 0,
    val selectedLanguage: String = "te",
    val farmerName: String = "Ravi Kumar",
    val selectedPondId: String = "pond_01",
    val isPlayingVoiceSample: Boolean = false,
    val voiceError: String? = null
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val audioPlayerManager: AudioPlayerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.getLanguage().collect { lang ->
                _uiState.value = _uiState.value.copy(selectedLanguage = lang)
            }
        }
        viewModelScope.launch {
            audioPlayerManager.playbackState.collect { state ->
                when (state) {
                    is PlaybackState.Loading, is PlaybackState.Playing -> {
                        _uiState.value = _uiState.value.copy(
                            isPlayingVoiceSample = true,
                            voiceError = null
                        )
                    }
                    is PlaybackState.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isPlayingVoiceSample = false,
                            voiceError = state.message
                        )
                    }
                    is PlaybackState.Idle, is PlaybackState.Completed, is PlaybackState.Paused -> {
                        _uiState.value = _uiState.value.copy(isPlayingVoiceSample = false)
                    }
                }
            }
        }
    }

    fun selectLanguage(language: String) {
        _uiState.value = _uiState.value.copy(selectedLanguage = language)
        viewModelScope.launch {
            preferencesRepository.setLanguage(language)
        }
    }

    fun updateFarmerName(name: String) {
        _uiState.value = _uiState.value.copy(farmerName = name)
    }

    fun selectPond(pondId: String) {
        _uiState.value = _uiState.value.copy(selectedPondId = pondId)
    }

    fun nextStep() {
        if (_uiState.value.currentStep < 4) {
            _uiState.value = _uiState.value.copy(currentStep = _uiState.value.currentStep + 1)
        }
    }

    fun previousStep() {
        if (_uiState.value.currentStep > 0) {
            _uiState.value = _uiState.value.copy(currentStep = _uiState.value.currentStep - 1)
        }
    }

    fun playVoiceTest(audioUri: String) {
        _uiState.value = _uiState.value.copy(isPlayingVoiceSample = true, voiceError = null)
        audioPlayerManager.play(audioUri, useAlarmStream = false)
    }

    fun stopVoiceTest() {
        audioPlayerManager.stop()
        _uiState.value = _uiState.value.copy(isPlayingVoiceSample = false)
    }

    fun completeOnboarding(onFinished: () -> Unit) {
        viewModelScope.launch {
            preferencesRepository.setFarmerName(_uiState.value.farmerName)
            preferencesRepository.setSelectedPondId(_uiState.value.selectedPondId)
            preferencesRepository.setOnboardingCompleted(true)
            onFinished()
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayerManager.stop()
    }
}
