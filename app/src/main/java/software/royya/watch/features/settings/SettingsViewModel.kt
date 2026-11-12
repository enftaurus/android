package software.royya.watch.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import software.royya.watch.audio.player.AudioPlayerManager
import software.royya.watch.domain.repository.PreferencesRepository
import javax.inject.Inject

data class SettingsUiState(
    val farmerName: String = "Ravi Kumar",
    val selectedLanguage: String = "te",
    val voiceAlertsEnabled: Boolean = true,
    val audioRepeatEnabled: Boolean = false,
    val isPlayingSample: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val audioPlayerManager: AudioPlayerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        viewModelScope.launch {
            audioPlayerManager.playbackState.collect { state ->
                val playing = state is software.royya.watch.audio.player.PlaybackState.Playing ||
                    state is software.royya.watch.audio.player.PlaybackState.Loading
                _uiState.value = _uiState.value.copy(isPlayingSample = playing)
            }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            preferencesRepository.getFarmerName().collect { name ->
                _uiState.value = _uiState.value.copy(farmerName = name)
            }
        }
        viewModelScope.launch {
            preferencesRepository.getLanguage().collect { lang ->
                _uiState.value = _uiState.value.copy(selectedLanguage = lang)
            }
        }
        viewModelScope.launch {
            preferencesRepository.getVoiceAlertPreference().collect { enabled ->
                _uiState.value = _uiState.value.copy(voiceAlertsEnabled = enabled)
            }
        }
        viewModelScope.launch {
            preferencesRepository.getAudioRepeatPreference().collect { repeat ->
                _uiState.value = _uiState.value.copy(audioRepeatEnabled = repeat)
            }
        }
    }

    fun setLanguage(language: String) {
        viewModelScope.launch {
            preferencesRepository.setLanguage(language)
        }
    }

    fun setVoiceAlerts(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setVoiceAlertPreference(enabled)
        }
    }

    fun setAudioRepeat(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAudioRepeatPreference(enabled)
        }
    }

    fun playSampleVoice(audioUri: String) {
        audioPlayerManager.play(audioUri)
    }

    fun stopSampleVoice() {
        audioPlayerManager.stop()
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayerManager.stop()
    }
}
