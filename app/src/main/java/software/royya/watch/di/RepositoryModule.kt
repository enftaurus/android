package software.royya.watch.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import software.royya.watch.core.datastore.PreferencesManager
import software.royya.watch.data.repository.RemoteAlertRepository
import software.royya.watch.data.repository.RemoteAudioRepository
import software.royya.watch.data.repository.RemotePondRepository
import software.royya.watch.domain.repository.AeratorRepository
import software.royya.watch.domain.repository.AlertRepository
import software.royya.watch.domain.repository.AudioRepository
import software.royya.watch.domain.repository.PondRepository
import software.royya.watch.domain.repository.PreferencesRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPondRepository(
        remotePondRepository: RemotePondRepository
    ): PondRepository

    @Binds
    @Singleton
    abstract fun bindAeratorRepository(
        remotePondRepository: RemotePondRepository
    ): AeratorRepository

    @Binds
    @Singleton
    abstract fun bindAlertRepository(
        remoteAlertRepository: RemoteAlertRepository
    ): AlertRepository

    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(
        preferencesManager: PreferencesManager
    ): PreferencesRepository

    @Binds
    @Singleton
    abstract fun bindAudioRepository(
        remoteAudioRepository: RemoteAudioRepository
    ): AudioRepository
}
