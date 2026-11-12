package software.royya.watch.data

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import software.royya.watch.data.remote.RoyyaApiService
import software.royya.watch.data.remote.dto.AcknowledgeResponseDto
import software.royya.watch.data.remote.dto.ActiveAlertDto
import software.royya.watch.data.remote.dto.ActiveAlertResponseDto
import software.royya.watch.data.remote.dto.AeratorManualRequestDto
import software.royya.watch.data.remote.dto.AeratorModeRequestDto
import software.royya.watch.data.remote.dto.AeratorStateDto
import software.royya.watch.data.remote.dto.SystemDataDto
import software.royya.watch.data.repository.RemoteAlertRepository
import software.royya.watch.data.repository.RemotePondRepository
import software.royya.watch.domain.model.AeratorMode
import software.royya.watch.domain.model.AeratorRelayState
import software.royya.watch.domain.model.Connectivity
import software.royya.watch.domain.model.HealthStatus
import software.royya.watch.domain.model.OxygenSource

class RemoteRepositoriesTest {

    @Test
    fun refreshNow_updatesPondReadingsAndAeratorState() = runBlocking {
        val api = mockk<RoyyaApiService>()
        coEvery { api.getSystemData() } returns Response.success(
            SystemDataDto(
                temperature = 28.5f,
                turbidity = 2.1f,
                dissolvedOxygen = 4.2f,
                mode = "AUTO",
                aeratorState = "ON",
                mockDoEnabled = true
            )
        )

        val repo = RemotePondRepository(api)
        val pond = repo.getPondSummary("pond_01")

        assertEquals(Connectivity.ONLINE, pond.connectivity)
        assertEquals(HealthStatus.CRITICAL, pond.healthStatus)
        assertEquals(48, pond.healthScore)

        val reading = repo.getLatestReadings("pond_01").first()
        assertNotNull(reading)
        assertEquals(4.2f, reading!!.dissolvedOxygen!!, 0.01f)
        assertEquals(28.5f, reading.temperature!!, 0.01f)
        assertEquals(2.1f, reading.turbidity!!, 0.01f)
        assertEquals(OxygenSource.SIMULATED, reading.oxygenSource)

        val aerator = repo.getAeratorState("pond_01").first()
        assertEquals(AeratorMode.AUTO, aerator.mode)
        assertEquals(AeratorRelayState.ON, aerator.state)
    }

    @Test
    fun setManualState_failsInAutoMode_andSucceedsInManualMode() = runBlocking {
        val api = mockk<RoyyaApiService>()
        coEvery { api.getSystemData() } returns Response.success(
            SystemDataDto(28.0f, 1.5f, 6.4f, "AUTO", "OFF", false)
        )
        coEvery { api.setAeratorMode(AeratorModeRequestDto("MANUAL")) } returns Response.success(
            AeratorStateDto("MANUAL", "OFF")
        )
        coEvery { api.setManualAerator(AeratorManualRequestDto("ON")) } returns Response.success(
            AeratorStateDto("MANUAL", "ON")
        )

        val repo = RemotePondRepository(api)
        repo.refreshNow()

        val autoResult = repo.setManualState("pond_01", AeratorRelayState.ON)
        assertTrue(autoResult.isFailure)

        val modeResult = repo.setMode("pond_01", AeratorMode.MANUAL)
        assertTrue(modeResult.isSuccess)
        assertEquals(AeratorMode.MANUAL, modeResult.getOrNull()?.mode)

        val manualResult = repo.setManualState("pond_01", AeratorRelayState.ON)
        assertTrue(manualResult.isSuccess)
        assertEquals(AeratorRelayState.ON, manualResult.getOrNull()?.state)
    }

    @Test
    fun remotePondRepository_marksOfflineOnHttpError() = runBlocking {
        val api = mockk<RoyyaApiService>()
        coEvery { api.getSystemData() } returns Response.error(503, "Unavailable".toResponseBody())

        val repo = RemotePondRepository(api)
        val pond = repo.getPondSummary("pond_01")

        assertEquals(Connectivity.OFFLINE, pond.connectivity)
        assertEquals(HealthStatus.OFFLINE, pond.healthStatus)
    }

    @Test
    fun pollActiveAlert_andAcknowledge_updatesActiveAlertFlow() = runBlocking {
        val api = mockk<RoyyaApiService>()
        coEvery { api.getSystemData() } returns Response.success(
            SystemDataDto(28.0f, 1.5f, 4.2f, "AUTO", "ON", true)
        )
        coEvery { api.getActiveAlert() } returns Response.success(
            ActiveAlertResponseDto(
                activeAlert = ActiveAlertDto(
                    id = "alert_123",
                    title = "అత్యవసర హెచ్చరిక",
                    message = "ఆక్సిజన్ పడిపోయింది",
                    severity = "CRITICAL",
                    dissolvedOxygen = 4.2f,
                    audioUrl = "/api/voice/alert"
                )
            )
        )
        coEvery { api.acknowledgeAlert() } returns Response.success(
            AcknowledgeResponseDto("success")
        )

        val pondRepo = RemotePondRepository(api)
        val alertRepo = RemoteAlertRepository(api, pondRepo)

        alertRepo.pollActiveAlert()
        assertEquals("alert_123", alertRepo.activeEmergencyAlert.value?.id)

        val acked = alertRepo.acknowledgeAlert("alert_123")
        assertTrue(acked)
        assertNull(alertRepo.activeEmergencyAlert.value)
        coVerify(exactly = 1) { api.acknowledgeAlert() }
    }
}
