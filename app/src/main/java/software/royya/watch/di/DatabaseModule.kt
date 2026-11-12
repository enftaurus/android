package software.royya.watch.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import software.royya.watch.data.local.RoyyaDatabase
import software.royya.watch.data.local.dao.AlertDao
import software.royya.watch.data.local.dao.PondDao
import software.royya.watch.data.local.dao.SensorReadingDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideRoyyaDatabase(
        @ApplicationContext context: Context
    ): RoyyaDatabase {
        return Room.databaseBuilder(
            context,
            RoyyaDatabase::class.java,
            "royya_watch.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun providePondDao(db: RoyyaDatabase): PondDao = db.pondDao()

    @Provides
    fun provideSensorReadingDao(db: RoyyaDatabase): SensorReadingDao = db.sensorReadingDao()

    @Provides
    fun provideAlertDao(db: RoyyaDatabase): AlertDao = db.alertDao()
}
