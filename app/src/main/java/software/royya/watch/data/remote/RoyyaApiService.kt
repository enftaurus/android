package software.royya.watch.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import software.royya.watch.data.remote.dto.*

interface RoyyaApiService {

    // --- Category A: Observed FastAPI Endpoints ---

    @GET("/api/data")
    suspend fun getSystemData(): Response<SystemDataDto>

    @GET("/api/aerator/state")
    suspend fun getAeratorState(): Response<AeratorStateDto>

    @POST("/api/aerator/mode")
    suspend fun setAeratorMode(
        @Body body: AeratorModeRequestDto
    ): Response<AeratorStateDto>

    @POST("/api/aerator/manual")
    suspend fun setManualAerator(
        @Body body: AeratorManualRequestDto
    ): Response<AeratorStateDto>

    @POST("/api/mock/do")
    suspend fun setMockDO(
        @Body body: MockDoRequestDto
    ): Response<SystemDataDto>

    // --- Category B: Active Emergency Alerts & Voice Call Keypad Control ---

    @GET("/api/alerts/active")
    suspend fun getActiveAlert(): Response<ActiveAlertResponseDto>

    @POST("/api/alerts/simulate")
    suspend fun simulateAlert(
        @Body body: SimulateAlertRequestDto
    ): Response<ActiveAlertDto>

    @POST("/api/alerts/acknowledge")
    suspend fun acknowledgeAlert(): Response<AcknowledgeResponseDto>

    @POST("/api/aerator/keypad")
    suspend fun keypadInput(
        @Body body: KeypadRequestDto
    ): Response<KeypadResponseDto>

    // --- Category C: Proposed Multi-Pond & Alert Endpoints ---

    @GET("/api/v1/alerts")
    suspend fun getAlerts(
        @Query("pond_id") pondId: String? = null
    ): Response<List<AlertDto>>

    @POST("/api/v1/alerts/{alert_id}/acknowledge")
    suspend fun acknowledgeAlertById(
        @retrofit2.http.Path("alert_id") alertId: String
    ): Response<AlertDto>
}
