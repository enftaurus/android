package software.royya.watch.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import software.royya.watch.BuildConfig
import software.royya.watch.domain.repository.AudioRepository
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteAudioRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : AudioRepository {

    override suspend fun getAudioUri(alertId: String, remoteUrl: String?): Result<String> {
        val baseUrl = BuildConfig.BACKEND_BASE_URL.trimEnd('/')
        val resolved = when {
            remoteUrl.isNullOrBlank() -> "$baseUrl/api/voice/alert"
            remoteUrl.startsWith("/") -> "$baseUrl$remoteUrl"
            else -> remoteUrl
        }
        return Result.success(resolved)
    }

    override suspend fun cacheAudio(alertId: String, remoteUrl: String): Result<String> {
        return getAudioUri(alertId, remoteUrl)
    }

    override suspend fun removeCachedAudio(alertId: String) {
        val cacheDir = File(context.cacheDir, "audio_alerts")
        File(cacheDir, "$alertId.mp3").delete()
    }
}
