package software.royya.watch.features.call

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import software.royya.watch.audio.player.AudioPlayerManager
import software.royya.watch.audio.player.PlaybackState
import software.royya.watch.data.remote.RoyyaApiService
import software.royya.watch.data.remote.dto.KeypadRequestDto
import software.royya.watch.data.remote.dto.KeypadResponseDto
import software.royya.watch.data.repository.RemotePondRepository
import software.royya.watch.domain.repository.AlertRepository

@OptIn(ExperimentalCoroutinesApi::class)
class EmergencyCallViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initializeAlert_startsRinging_andDoesNotSpeakUntilAnswered() = runTest {
        val api = mockk<RoyyaApiService>()
        val audioPlayer = mockk<AudioPlayerManager>(relaxed = true)
        every { audioPlayer.playbackState } returns MutableStateFlow(PlaybackState.Idle)
        val ringer = mockk<EmergencyCallRinger>(relaxed = true)
        val alertRepo = mockk<AlertRepository>(relaxed = true)
        val pondRepo = mockk<RemotePondRepository>(relaxed = true)

        val vm = EmergencyCallViewModel(api, audioPlayer, ringer, alertRepo, pondRepo)

        vm.initializeAlert(
            alertId = "alert_1",
            title = "అత్యవసర హెచ్చరిక",
            message = "ఆక్సిజన్ తగ్గింది",
            dissolvedOxygen = 4.2f,
            audioUrl = "/api/voice/alert"
        )

        assertEquals(CallStage.RINGING, vm.uiState.value.callStage)
        verify(exactly = 1) { ringer.startRinging() }
        verify(exactly = 0) { audioPlayer.play(any(), any()) }

        vm.answerCall()

        assertEquals(CallStage.ANSWERED, vm.uiState.value.callStage)
        verify(exactly = 1) { ringer.stopRinging() }
        verify(exactly = 1) { audioPlayer.play(match { it.endsWith("/api/voice/alert") }, true) }
    }

    @Test
    fun pressKeypad_stopsAudio_sendsCommand_andDismisses() = runTest {
        val api = mockk<RoyyaApiService>()
        coEvery { api.keypadInput(KeypadRequestDto("1")) } returns Response.success(
            KeypadResponseDto(
                status = "success",
                keyPressed = "1",
                mode = "MANUAL",
                aeratorState = "ON",
                message = "Aerator commanded ON"
            )
        )
        val audioPlayer = mockk<AudioPlayerManager>(relaxed = true)
        every { audioPlayer.playbackState } returns MutableStateFlow(PlaybackState.Playing(0L, 5000L))
        val ringer = mockk<EmergencyCallRinger>(relaxed = true)
        val alertRepo = mockk<AlertRepository>(relaxed = true)
        coEvery { alertRepo.acknowledgeAlert("alert_1") } returns true
        val pondRepo = mockk<RemotePondRepository>(relaxed = true)

        val vm = EmergencyCallViewModel(api, audioPlayer, ringer, alertRepo, pondRepo)
        vm.initializeAlert("alert_1", "Title", "Message", 4.1f, "/api/voice/alert")
        vm.answerCall()
        vm.pressKeypad("1")
        advanceUntilIdle()

        verify(atLeast = 1) { ringer.stopRinging() }
        verify(atLeast = 1) { audioPlayer.stop() }
        coVerify(exactly = 1) { api.keypadInput(KeypadRequestDto("1")) }
        coVerify(exactly = 1) { alertRepo.acknowledgeAlert("alert_1") }
        assertTrue(vm.uiState.value.shouldDismiss)
    }
}
