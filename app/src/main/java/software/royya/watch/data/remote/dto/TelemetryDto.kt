package software.royya.watch.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SystemDataDto(
    @SerialName("temperature") val temperature: Float,
    @SerialName("turbidity") val turbidity: Float,
    @SerialName("dissolved_oxygen") val dissolvedOxygen: Float,
    @SerialName("mode") val mode: String,
    @SerialName("aerator_state") val aeratorState: String,
    @SerialName("mock_do_enabled") val mockDoEnabled: Boolean? = false
)

@Serializable
data class AeratorStateDto(
    @SerialName("mode") val mode: String,
    @SerialName("state") val state: String
)

@Serializable
data class AeratorModeRequestDto(
    @SerialName("mode") val mode: String
)

@Serializable
data class AeratorManualRequestDto(
    @SerialName("state") val state: String
)

@Serializable
data class MockDoRequestDto(
    @SerialName("enabled") val enabled: Boolean,
    @SerialName("dissolved_oxygen") val dissolvedOxygen: Float
)

@Serializable
data class ActiveAlertDto(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("message") val message: String,
    @SerialName("severity") val severity: String = "CRITICAL",
    @SerialName("dissolved_oxygen") val dissolvedOxygen: Float? = null,
    @SerialName("audio_url") val audioUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("status") val status: String? = "NEW"
)

@Serializable
data class ActiveAlertResponseDto(
    @SerialName("active_alert") val activeAlert: ActiveAlertDto? = null
)

@Serializable
data class KeypadRequestDto(
    @SerialName("key") val key: String
)

@Serializable
data class KeypadResponseDto(
    @SerialName("status") val status: String,
    @SerialName("key_pressed") val keyPressed: String,
    @SerialName("mode") val mode: String,
    @SerialName("aerator_state") val aeratorState: String,
    @SerialName("message") val message: String? = null
)

@Serializable
data class AcknowledgeResponseDto(
    @SerialName("status") val status: String
)

@Serializable
data class SimulateAlertRequestDto(
    @SerialName("title") val title: String = "అత్యవసర హెచ్చరిక: తక్కువ ఆక్సిజన్",
    @SerialName("message") val message: String = "మీ చెరువులో ఆక్సిజన్ స్థాయి పడిపోయింది. ఏరేటర్ ఆన్ చేయడానికి 1 నొక్కండి, ఏరేటర్ ఆఫ్ చేయడానికి 2 నొక్కండి.",
    @SerialName("dissolved_oxygen") val dissolvedOxygen: Float = 4.2f,
    @SerialName("severity") val severity: String = "CRITICAL"
)

@Serializable
data class AlertDto(
    @SerialName("id") val id: String,
    @SerialName("pond_id") val pondId: String = "pond_01",
    @SerialName("severity") val severity: String,
    @SerialName("type") val type: String = "LOW_DISSOLVED_OXYGEN",
    @SerialName("title") val title: String,
    @SerialName("message") val message: String,
    @SerialName("parameter") val parameter: String? = null,
    @SerialName("value") val value: String? = null,
    @SerialName("recommendation") val recommendation: String? = null,
    @SerialName("audio_reference") val audioReference: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("status") val status: String
)
