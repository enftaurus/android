package software.royya.watch.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import software.royya.watch.BuildConfig
import software.royya.watch.data.remote.RoyyaApiService
import software.royya.watch.data.remote.dto.ActiveAlertDto
import software.royya.watch.data.remote.dto.SimulateAlertRequestDto
import software.royya.watch.domain.model.Alert
import software.royya.watch.domain.model.AlertSeverity
import software.royya.watch.domain.model.AlertStatus
import software.royya.watch.domain.model.AlertType
import software.royya.watch.domain.model.Connectivity
import software.royya.watch.domain.repository.AlertRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteAlertRepository @Inject constructor(
    private val apiService: RoyyaApiService,
    private val remotePondRepository: RemotePondRepository
) : AlertRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pollingStarted = AtomicBoolean(false)

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private val _activeEmergencyAlert = MutableStateFlow<ActiveAlertDto?>(null)
    val activeEmergencyAlert: StateFlow<ActiveAlertDto?> = _activeEmergencyAlert.asStateFlow()

    private val remoteAlertsState = MutableStateFlow<List<Alert>>(emptyList())
    private val acknowledgedIds = mutableSetOf<String>()
    private val resolvedIds = mutableSetOf<String>()

    fun ensurePollingStarted() {
        if (pollingStarted.compareAndSet(false, true)) {
            scope.launch {
                while (isActive) {
                    pollActiveAlert()
                    delay(2000L)
                }
            }
        }
    }

    suspend fun pollActiveAlert() {
        try {
            val response = apiService.getActiveAlert()
            if (response.isSuccessful) {
                val active = response.body()?.activeAlert
                if (active != null && !acknowledgedIds.contains(active.id)) {
                    _activeEmergencyAlert.value = active
                    upsertAlert(active.toDomainAlert())
                } else if (active == null) {
                    _activeEmergencyAlert.value = null
                }
            }
        } catch (_: Exception) {
            // Keep existing alert state when momentarily unreachable
        }
    }

    private fun upsertAlert(alert: Alert) {
        val current = remoteAlertsState.value
        val existingIdx = current.indexOfFirst { it.id == alert.id }
        remoteAlertsState.value = if (existingIdx >= 0) {
            current.toMutableList().apply { set(existingIdx, alert) }
        } else {
            listOf(alert) + current
        }
    }

    private fun ActiveAlertDto.toDomainAlert(): Alert {
        val baseUrl = BuildConfig.BACKEND_BASE_URL.trimEnd('/')
        val resolvedAudioUrl = when {
            audioUrl.isNullOrBlank() -> "$baseUrl/api/voice/alert"
            audioUrl.startsWith("/") -> "$baseUrl$audioUrl"
            else -> audioUrl
        }
        val parsedSeverity = when (severity.uppercase(Locale.US)) {
            "CRITICAL" -> AlertSeverity.CRITICAL
            "WARNING" -> AlertSeverity.WARNING
            else -> AlertSeverity.INFO
        }
        val parsedStatus = when {
            resolvedIds.contains(id) -> AlertStatus.RESOLVED
            acknowledgedIds.contains(id) || status.equals("ACKNOWLEDGED", ignoreCase = true) -> AlertStatus.ACKNOWLEDGED
            else -> AlertStatus.NEW
        }
        return Alert(
            id = id,
            pondId = "pond_01",
            pondName = "Pond 01 (ఉత్తర చెరువు)",
            severity = parsedSeverity,
            type = AlertType.LOW_DISSOLVED_OXYGEN,
            title = title,
            message = message,
            parameter = "Dissolved Oxygen",
            value = dissolvedOxygen?.let { "%.2f mg/L".format(it) },
            recommendation = "ఏరేటర్ ఆన్ చేయడానికి 1 నొక్కండి, ఆఫ్ చేయడానికి 2 నొక్కండి.",
            language = "te-IN",
            audioReference = resolvedAudioUrl,
            createdAt = createdAt ?: isoFormat.format(Date()),
            status = parsedStatus
        )
    }

    private fun buildTelemetryAlerts(
        connectivity: Connectivity,
        doValue: Float?,
        tempValue: Float?,
        turbidityValue: Float?
    ): List<Alert> {
        val now = isoFormat.format(Date())
        val baseUrl = BuildConfig.BACKEND_BASE_URL.trimEnd('/')
        val list = mutableListOf<Alert>()

        if (connectivity == Connectivity.OFFLINE) {
            val id = "telemetry_offline"
            list.add(
                Alert(
                    id = id,
                    pondId = "pond_01",
                    pondName = "Pond 01 (ఉత్తర చెరువు)",
                    severity = AlertSeverity.CRITICAL,
                    type = AlertType.DEVICE_OFFLINE,
                    title = "బ్యాకెండ్ కనెక్షన్ విఫలమైంది (Backend Offline)",
                    message = "FastAPI సర్వర్‌ను సంప్రదించలేకపోతున్నాము (${BuildConfig.BACKEND_BASE_URL}). సెన్సార్ డేటా అందుబాటులో లేదు.",
                    parameter = "Connectivity",
                    value = "Offline",
                    recommendation = "సర్వర్ మరియు ESP32 విద్యుత్ కనెక్టివిటీని తనిఖీ చేయండి.",
                    createdAt = now,
                    status = if (acknowledgedIds.contains(id)) AlertStatus.ACKNOWLEDGED else AlertStatus.NEW
                )
            )
        }

        if (doValue != null && doValue < 5.0f && connectivity == Connectivity.ONLINE) {
            val id = "telemetry_do_low"
            list.add(
                Alert(
                    id = id,
                    pondId = "pond_01",
                    pondName = "Pond 01 (ఉత్తర చెరువు)",
                    severity = AlertSeverity.CRITICAL,
                    type = AlertType.LOW_DISSOLVED_OXYGEN,
                    title = "తక్కువ ఆక్సిజన్ హెచ్చరిక (%.2f mg/L)".format(doValue),
                    message = "ఆక్సిజన్ పరిమితి 5.0 mg/L కంటే తగ్గింది. రొయ్యల ఆరోగ్యానికి ముప్పు కలగవచ్చు.",
                    parameter = "Dissolved Oxygen",
                    value = "%.2f mg/L".format(doValue),
                    recommendation = "ఆటో మోడ్‌లో ఏరేటర్ ఆన్‌లో ఉంది. పాడిల్ వీల్స్ తిరుగుతున్నాయో లేదో సరిచూడండి.",
                    audioReference = "$baseUrl/api/voice/alert",
                    createdAt = now,
                    status = if (acknowledgedIds.contains(id)) AlertStatus.ACKNOWLEDGED else AlertStatus.NEW
                )
            )
        }

        if (tempValue != null && (tempValue > 32.0f || tempValue < 24.0f) && connectivity == Connectivity.ONLINE) {
            val id = "telemetry_temp_abnormal"
            list.add(
                Alert(
                    id = id,
                    pondId = "pond_01",
                    pondName = "Pond 01 (ఉత్తర చెరువు)",
                    severity = AlertSeverity.WARNING,
                    type = AlertType.HIGH_TEMPERATURE,
                    title = "అసాధారణ ఉష్ణోగ్రత (%.2f°C)".format(tempValue),
                    message = "నీటి ఉష్ణోగ్రత సాధారణ పరిధి (26–32°C) నుండి వైదొలిగింది.",
                    parameter = "Temperature",
                    value = "%.2f°C".format(tempValue),
                    recommendation = "చెరువు నీటి లోతును మరియు నీడను తనిఖీ చేయండి.",
                    createdAt = now,
                    status = if (acknowledgedIds.contains(id)) AlertStatus.ACKNOWLEDGED else AlertStatus.NEW
                )
            )
        }

        if (turbidityValue != null && turbidityValue > 3.5f && connectivity == Connectivity.ONLINE) {
            val id = "telemetry_turbidity_high"
            list.add(
                Alert(
                    id = id,
                    pondId = "pond_01",
                    pondName = "Pond 01 (ఉత్తర చెరువు)",
                    severity = AlertSeverity.WARNING,
                    type = AlertType.WATER_QUALITY_WARNING,
                    title = "అధిక మడ్డితనం వోల్టేజ్ (%.2fV)".format(turbidityValue),
                    message = "సెన్సార్ వోల్టేజ్ పెరిగింది. నీటి స్వచ్ఛత తగ్గింది.",
                    parameter = "Turbidity",
                    value = "%.2fV".format(turbidityValue),
                    recommendation = "ఫీడ్ పరిమాణం మరియు చెరువు అడుగుభాగాన్ని గమనించండి.",
                    createdAt = now,
                    status = if (acknowledgedIds.contains(id)) AlertStatus.ACKNOWLEDGED else AlertStatus.NEW
                )
            )
        }

        return list
    }

    override fun observeAlerts(): Flow<List<Alert>> {
        ensurePollingStarted()
        return combine(
            remoteAlertsState,
            remotePondRepository.observeConnectivity(),
            remotePondRepository.getLatestReadings("pond_01")
        ) { serverAlerts, connectivity, reading ->
            val liveTelemetryAlerts = buildTelemetryAlerts(
                connectivity = connectivity,
                doValue = reading?.dissolvedOxygen,
                tempValue = reading?.temperature,
                turbidityValue = reading?.turbidity
            )
            val combined = (serverAlerts + liveTelemetryAlerts).distinctBy { it.id }
            if (combined.isEmpty()) {
                listOf(
                    Alert(
                        id = "baseline_ok",
                        pondId = "pond_01",
                        pondName = "Pond 01 (ఉత్తర చెరువు)",
                        severity = AlertSeverity.INFO,
                        type = AlertType.SYSTEM_WARNING,
                        title = "సిస్టమ్ స్థిరంగా ఉంది",
                        message = "ఆటోమేటిక్ థ్రెషోల్డ్‌లు (5.0 mg/L ఆన్ / 6.0 mg/L ఆఫ్) సక్రమంగా పనిచేస్తున్నాయి.",
                        parameter = "Automation",
                        value = "OK",
                        recommendation = "ఎటువంటి ప్రత్యేక చర్య అవసరం లేదు.",
                        createdAt = isoFormat.format(Date()),
                        status = AlertStatus.RESOLVED
                    )
                )
            } else {
                combined
            }
        }
    }

    override fun getAlerts(pondId: String?): Flow<List<Alert>> {
        return observeAlerts().map { list ->
            if (pondId != null) list.filter { it.pondId == pondId } else list
        }
    }

    override suspend fun getAlert(alertId: String): Alert? {
        val currentActive = _activeEmergencyAlert.value
        if (currentActive?.id == alertId) {
            return currentActive.toDomainAlert()
        }
        return remoteAlertsState.value.firstOrNull { it.id == alertId }
    }

    override suspend fun acknowledgeAlert(alertId: String): Boolean {
        acknowledgedIds.add(alertId)
        val now = isoFormat.format(Date())
        remoteAlertsState.value = remoteAlertsState.value.map {
            if (it.id == alertId) it.copy(status = AlertStatus.ACKNOWLEDGED, acknowledgedAt = now)
            else it
        }
        if (_activeEmergencyAlert.value?.id == alertId || alertId == "active") {
            _activeEmergencyAlert.value = null
        }
        return try {
            val response = apiService.acknowledgeAlert()
            _activeEmergencyAlert.value = null
            response.isSuccessful
        } catch (_: Exception) {
            true
        }
    }

    override suspend fun markAlertResolved(alertId: String): Boolean {
        resolvedIds.add(alertId)
        val now = isoFormat.format(Date())
        remoteAlertsState.value = remoteAlertsState.value.map {
            if (it.id == alertId) it.copy(status = AlertStatus.RESOLVED, resolvedAt = now)
            else it
        }
        return true
    }

    override suspend fun triggerSimulatedAlert(alert: Alert) {
        upsertAlert(alert)
        try {
            val resp = apiService.simulateAlert(
                SimulateAlertRequestDto(
                    title = alert.title,
                    message = alert.message,
                    dissolvedOxygen = 4.2f,
                    severity = alert.severity.name
                )
            )
            val created = resp.body()
            if (resp.isSuccessful && created != null) {
                _activeEmergencyAlert.value = created
                upsertAlert(created.toDomainAlert())
                remotePondRepository.refreshNow()
            }
        } catch (_: Exception) {
            // Ignored if offline
        }
    }
}
