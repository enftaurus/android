package software.royya.watch.domain.model

enum class Connectivity {
    ONLINE,
    OFFLINE,
    UNKNOWN
}

enum class HealthStatus {
    HEALTHY,
    ATTENTION,
    CRITICAL,
    OFFLINE,
    UNKNOWN
}

enum class OxygenSource {
    MEASURED,
    PREDICTED,
    SIMULATED,
    UNKNOWN
}

enum class AeratorRelayState {
    ON,
    OFF,
    UNKNOWN
}

enum class AeratorMode {
    AUTO,
    MANUAL,
    UNKNOWN
}

enum class AlertSeverity {
    INFO,
    WARNING,
    CRITICAL
}

enum class AlertStatus {
    NEW,
    ACKNOWLEDGED,
    RESOLVED
}

enum class AlertType {
    LOW_DISSOLVED_OXYGEN,
    HIGH_TEMPERATURE,
    WATER_QUALITY_WARNING,
    AERATOR_FAILURE,
    DEVICE_OFFLINE,
    SYSTEM_WARNING
}

data class Pond(
    val id: String,
    val name: String,
    val location: String,
    val deviceId: String,
    val connectivity: Connectivity,
    val healthStatus: HealthStatus,
    val healthScore: Int? = null,
    val lastUpdated: String
)

data class SensorReading(
    val pondId: String,
    val temperature: Float?,
    val turbidity: Float?,
    val tds: Float? = null,
    val ph: Float? = null,
    val dissolvedOxygen: Float?,
    val oxygenSource: OxygenSource,
    val timestamp: String
)

data class AeratorStatus(
    val pondId: String,
    val mode: AeratorMode,
    val state: AeratorRelayState,
    val lastStateChange: String? = null,
    val pending: Boolean = false
)

data class Alert(
    val id: String,
    val pondId: String,
    val pondName: String = "Pond 01",
    val severity: AlertSeverity,
    val type: AlertType,
    val title: String,
    val message: String,
    val parameter: String? = null,
    val value: String? = null,
    val recommendation: String? = null,
    val language: String = "te-IN",
    val audioReference: String? = null,
    val createdAt: String,
    val acknowledgedAt: String? = null,
    val resolvedAt: String? = null,
    val status: AlertStatus
)
