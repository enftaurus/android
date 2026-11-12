package software.royya.watch

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import software.royya.watch.di.NetworkModule

@RunWith(AndroidJUnit4::class)
class BackendReachabilityInstrumentedTest {

    private fun createClientAndApi() = run {
        val json = NetworkModule.provideJson()
        val okHttp = NetworkModule.provideOkHttpClient()
        val retrofit = NetworkModule.provideRetrofit(okHttp, json)
        val api = NetworkModule.provideRoyyaApiService(retrofit)
        okHttp to api
    }

    @Test
    fun devBuildConfig_pointsToReachableLanBackend() {
        assertTrue(
            "BACKEND_BASE_URL must not be blank",
            BuildConfig.BACKEND_BASE_URL.startsWith("http://") ||
                BuildConfig.BACKEND_BASE_URL.startsWith("https://")
        )
    }

    @Test
    fun androidPhone_canReachSystemDataAndAeratorAndAlertsEndpoints() = runBlocking {
        val (_, api) = createClientAndApi()

        val dataResp = api.getSystemData()
        assertTrue(
            "Failed to reach ${BuildConfig.BACKEND_BASE_URL}/api/data: HTTP ${dataResp.code()}",
            dataResp.isSuccessful
        )
        assertNotNull("Empty /api/data body", dataResp.body())

        val aeratorResp = api.getAeratorState()
        assertTrue(
            "Failed to reach /api/aerator/state: HTTP ${aeratorResp.code()}",
            aeratorResp.isSuccessful
        )
        val aeratorBody = aeratorResp.body()
        assertNotNull(aeratorBody)
        assertTrue(aeratorBody!!.mode in listOf("AUTO", "MANUAL"))
        assertTrue(aeratorBody.state in listOf("ON", "OFF"))

        val activeAlertResp = api.getActiveAlert()
        assertTrue(
            "Failed to reach /api/alerts/active: HTTP ${activeAlertResp.code()}",
            activeAlertResp.isSuccessful
        )
        assertNotNull(activeAlertResp.body())
    }

    @Test
    fun androidPhone_canStreamTeluguVoiceAlertMp3() {
        val (okHttp, _) = createClientAndApi()
        val audioUrl = "${BuildConfig.BACKEND_BASE_URL.trimEnd('/')}/api/voice/alert"
        val request = Request.Builder().url(audioUrl).get().build()

        okHttp.newCall(request).execute().use { response ->
            assertEquals("Expected HTTP 200 for $audioUrl", 200, response.code)
            val contentType = response.header("Content-Type") ?: ""
            assertTrue(
                "Expected audio/mpeg content type, got: $contentType",
                contentType.contains("audio/mpeg")
            )
            val bytes = response.body?.bytes() ?: ByteArray(0)
            assertTrue(
                "Expected non-empty Telugu alert MP3 (> 10KB), got ${bytes.size} bytes",
                bytes.size > 10_000
            )
        }
    }
}
