package software.royya.watch.domain.repository

import kotlinx.coroutines.flow.Flow
import software.royya.watch.domain.model.*

interface PondRepository {
    fun observePondSummary(pondId: String): Flow<Pond>
    suspend fun getPondSummary(pondId: String): Pond
    suspend fun getPonds(): List<Pond>
    fun getLatestReadings(pondId: String): Flow<SensorReading?>
    suspend fun getSensorHistory(pondId: String, range: String): List<SensorReading>
    fun getAeratorState(pondId: String): Flow<AeratorStatus>
    fun observeConnectivity(): Flow<Connectivity>
}

interface AlertRepository {
    fun getAlerts(pondId: String? = null): Flow<List<Alert>>
    suspend fun getAlert(alertId: String): Alert?
    suspend fun acknowledgeAlert(alertId: String): Boolean
    fun observeAlerts(): Flow<List<Alert>>
    suspend fun markAlertResolved(alertId: String): Boolean
    // For simulation testing
    suspend fun triggerSimulatedAlert(alert: Alert)
}

interface AeratorRepository {
    fun getState(pondId: String): Flow<AeratorStatus>
    suspend fun setMode(pondId: String, mode: AeratorMode): Result<AeratorStatus>
    suspend fun setManualState(pondId: String, state: AeratorRelayState): Result<AeratorStatus>
}

interface PreferencesRepository {
    fun getLanguage(): Flow<String>
    suspend fun setLanguage(language: String)
    fun getVoiceAlertPreference(): Flow<Boolean>
    suspend fun setVoiceAlertPreference(enabled: Boolean)
    fun getAudioRepeatPreference(): Flow<Boolean>
    suspend fun setAudioRepeatPreference(enabled: Boolean)
    fun isOnboardingCompleted(): Flow<Boolean>
    suspend fun setOnboardingCompleted(completed: Boolean)
    fun getFarmerName(): Flow<String>
    suspend fun setFarmerName(name: String)
    fun getSelectedPondId(): Flow<String>
    suspend fun setSelectedPondId(pondId: String)
}

interface AudioRepository {
    suspend fun getAudioUri(alertId: String, remoteUrl: String? = null): Result<String>
    suspend fun cacheAudio(alertId: String, remoteUrl: String): Result<String>
    suspend fun removeCachedAudio(alertId: String)
}
