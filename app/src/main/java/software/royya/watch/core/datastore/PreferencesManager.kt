package software.royya.watch.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import software.royya.watch.domain.repository.PreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "royya_preferences")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) : PreferencesRepository {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("app_language")
        val VOICE_ALERTS = booleanPreferencesKey("voice_alerts_enabled")
        val AUDIO_REPEAT = booleanPreferencesKey("audio_repeat_enabled")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val FARMER_NAME = stringPreferencesKey("farmer_name")
        val SELECTED_POND_ID = stringPreferencesKey("selected_pond_id")
    }

    override fun getLanguage(): Flow<String> = context.dataStore.data.map { preferences ->
        preferences[Keys.LANGUAGE] ?: "te"
    }

    override suspend fun setLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.LANGUAGE] = language
        }
    }

    override fun getVoiceAlertPreference(): Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.VOICE_ALERTS] ?: true
    }

    override suspend fun setVoiceAlertPreference(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.VOICE_ALERTS] = enabled
        }
    }

    override fun getAudioRepeatPreference(): Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.AUDIO_REPEAT] ?: false
    }

    override suspend fun setAudioRepeatPreference(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.AUDIO_REPEAT] = enabled
        }
    }

    override fun isOnboardingCompleted(): Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.ONBOARDING_COMPLETED] ?: false
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.ONBOARDING_COMPLETED] = completed
        }
    }

    override fun getFarmerName(): Flow<String> = context.dataStore.data.map { preferences ->
        preferences[Keys.FARMER_NAME] ?: "Ravi Kumar"
    }

    override suspend fun setFarmerName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.FARMER_NAME] = name
        }
    }

    override fun getSelectedPondId(): Flow<String> = context.dataStore.data.map { preferences ->
        preferences[Keys.SELECTED_POND_ID] ?: "pond_01"
    }

    override suspend fun setSelectedPondId(pondId: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.SELECTED_POND_ID] = pondId
        }
    }
}
