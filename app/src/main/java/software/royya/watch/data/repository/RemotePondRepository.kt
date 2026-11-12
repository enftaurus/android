package software.royya.watch.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import software.royya.watch.data.remote.RoyyaApiService
import software.royya.watch.data.remote.dto.AeratorManualRequestDto
import software.royya.watch.data.remote.dto.AeratorModeRequestDto
import software.royya.watch.data.remote.dto.SystemDataDto
import software.royya.watch.domain.model.AeratorMode
import software.royya.watch.domain.model.AeratorRelayState
import software.royya.watch.domain.model.AeratorStatus
import software.royya.watch.domain.model.Connectivity
import software.royya.watch.domain.model.HealthStatus
import software.royya.watch.domain.model.OxygenSource
import software.royya.watch.domain.model.Pond
import software.royya.watch.domain.model.SensorReading
import software.royya.watch.domain.repository.AeratorRepository
import software.royya.watch.domain.repository.PondRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemotePondRepository @Inject constructor(
    private val apiService: RoyyaApiService
) : PondRepository, AeratorRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pollingStarted = AtomicBoolean(false)

    private val displayTimeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    private val defaultPond = Pond(
        id = "pond_01",
        name = "Pond 01 (ఉత్తర చెరువు)",
        location = "భీమవరం క్లస్టర్ A",
        deviceId = "ESP32-ROYYA-01",
        connectivity = Connectivity.UNKNOWN,
        healthStatus = HealthStatus.UNKNOWN,
        healthScore = null,
        lastUpdated = "--"
    )

    private val pondState = MutableStateFlow(defaultPond)
    private val latestReadingState = MutableStateFlow<SensorReading?>(null)
    private val aeratorState = MutableStateFlow(
        AeratorStatus(
            pondId = "pond_01",
            mode = AeratorMode.AUTO,
            state = AeratorRelayState.OFF
        )
    )
    private val connectivityState = MutableStateFlow(Connectivity.UNKNOWN)
    private val historyLock = Any()
    private val historyBuffer = mutableListOf<SensorReading>()

    private fun ensurePollingStarted() {
        if (pollingStarted.compareAndSet(false, true)) {
            scope.launch {
                while (isActive) {
                    refreshNow()
                    delay(2500L)
                }
            }
        }
    }

    suspend fun refreshNow() {
        try {
            val response = apiService.getSystemData()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                applySystemData(body)
            } else {
                markOffline()
            }
        } catch (_: Exception) {
            markOffline()
        }
    }

    private fun applySystemData(dto: SystemDataDto) {
        val nowStr = displayTimeFormat.format(Date())
        val parsedMode = when (dto.mode.uppercase(Locale.US)) {
            "AUTO" -> AeratorMode.AUTO
            "MANUAL" -> AeratorMode.MANUAL
            else -> AeratorMode.UNKNOWN
        }
        val parsedState = when (dto.aeratorState.uppercase(Locale.US)) {
            "ON" -> AeratorRelayState.ON
            "OFF" -> AeratorRelayState.OFF
            else -> AeratorRelayState.UNKNOWN
        }

        val oxygenSource = if (dto.mockDoEnabled == true) {
            OxygenSource.SIMULATED
        } else {
            OxygenSource.PREDICTED
        }

        val reading = SensorReading(
            pondId = "pond_01",
            temperature = dto.temperature,
            turbidity = dto.turbidity,
            dissolvedOxygen = dto.dissolvedOxygen,
            oxygenSource = oxygenSource,
            timestamp = nowStr
        )

        val (healthScore, healthStatus) = computeHealth(dto.dissolvedOxygen, dto.temperature)

        connectivityState.value = Connectivity.ONLINE
        latestReadingState.value = reading
        aeratorState.value = AeratorStatus(
            pondId = "pond_01",
            mode = parsedMode,
            state = parsedState,
            lastStateChange = nowStr
        )
        pondState.value = defaultPond.copy(
            connectivity = Connectivity.ONLINE,
            healthStatus = healthStatus,
            healthScore = healthScore,
            lastUpdated = nowStr
        )

        synchronized(historyLock) {
            historyBuffer.add(reading)
            if (historyBuffer.size > 100) {
                historyBuffer.removeAt(0)
            }
        }
    }

    private fun markOffline() {
        connectivityState.value = Connectivity.OFFLINE
        pondState.value = pondState.value.copy(
            connectivity = Connectivity.OFFLINE,
            healthStatus = HealthStatus.OFFLINE
        )
    }

    private fun computeHealth(dissolvedOxygen: Float, temperature: Float): Pair<Int, HealthStatus> {
        return when {
            dissolvedOxygen >= 6.0f && temperature in 26f..32f -> 94 to HealthStatus.HEALTHY
            dissolvedOxygen >= 5.0f -> 78 to HealthStatus.ATTENTION
            else -> 48 to HealthStatus.CRITICAL
        }
    }

    override fun observePondSummary(pondId: String): Flow<Pond> {
        ensurePollingStarted()
        return pondState.asStateFlow()
    }

    override suspend fun getPondSummary(pondId: String): Pond {
        refreshNow()
        return pondState.value
    }

    override suspend fun getPonds(): List<Pond> {
        refreshNow()
        return listOf(pondState.value)
    }

    override fun getLatestReadings(pondId: String): Flow<SensorReading?> {
        ensurePollingStarted()
        return latestReadingState.asStateFlow()
    }

    override suspend fun getSensorHistory(pondId: String, range: String): List<SensorReading> {
        ensurePollingStarted()
        if (historyBuffer.isEmpty()) {
            refreshNow()
        }
        return synchronized(historyLock) {
            historyBuffer.toList()
        }
    }

    override fun getAeratorState(pondId: String): Flow<AeratorStatus> {
        ensurePollingStarted()
        return aeratorState.asStateFlow()
    }

    override fun observeConnectivity(): Flow<Connectivity> {
        ensurePollingStarted()
        return connectivityState.asStateFlow()
    }

    override fun getState(pondId: String): Flow<AeratorStatus> = getAeratorState(pondId)

    override suspend fun setMode(pondId: String, mode: AeratorMode): Result<AeratorStatus> {
        val modeStr = when (mode) {
            AeratorMode.AUTO -> "AUTO"
            AeratorMode.MANUAL -> "MANUAL"
            AeratorMode.UNKNOWN -> return Result.failure(IllegalArgumentException("Invalid mode"))
        }
        return try {
            val response = apiService.setAeratorMode(AeratorModeRequestDto(modeStr))
            val body = response.body()
            if (response.isSuccessful && body != null) {
                val updated = AeratorStatus(
                    pondId = pondId,
                    mode = if (body.mode.equals("AUTO", ignoreCase = true)) AeratorMode.AUTO else AeratorMode.MANUAL,
                    state = if (body.state.equals("ON", ignoreCase = true)) AeratorRelayState.ON else AeratorRelayState.OFF,
                    lastStateChange = displayTimeFormat.format(Date())
                )
                aeratorState.value = updated
                Result.success(updated)
            } else {
                Result.failure(IllegalStateException("Failed to set mode (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setManualState(pondId: String, state: AeratorRelayState): Result<AeratorStatus> {
        if (aeratorState.value.mode == AeratorMode.AUTO) {
            return Result.failure(IllegalStateException("Cannot change relay state while in AUTO mode."))
        }
        val stateStr = when (state) {
            AeratorRelayState.ON -> "ON"
            AeratorRelayState.OFF -> "OFF"
            AeratorRelayState.UNKNOWN -> return Result.failure(IllegalArgumentException("Invalid relay state"))
        }
        return try {
            val response = apiService.setManualAerator(AeratorManualRequestDto(stateStr))
            val body = response.body()
            if (response.isSuccessful && body != null) {
                val updated = AeratorStatus(
                    pondId = pondId,
                    mode = if (body.mode.equals("AUTO", ignoreCase = true)) AeratorMode.AUTO else AeratorMode.MANUAL,
                    state = if (body.state.equals("ON", ignoreCase = true)) AeratorRelayState.ON else AeratorRelayState.OFF,
                    lastStateChange = displayTimeFormat.format(Date())
                )
                aeratorState.value = updated
                Result.success(updated)
            } else {
                Result.failure(IllegalStateException("Failed to toggle aerator (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
