package software.royya.watch.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import software.royya.watch.data.local.entity.AlertEntity
import software.royya.watch.data.local.entity.PondEntity
import software.royya.watch.data.local.entity.SensorReadingEntity

@Dao
interface PondDao {
    @Query("SELECT * FROM ponds WHERE id = :pondId")
    fun observePond(pondId: String): Flow<PondEntity?>

    @Query("SELECT * FROM ponds")
    suspend fun getAllPonds(): List<PondEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPonds(ponds: List<PondEntity>)
}

@Dao
interface SensorReadingDao {
    @Query("SELECT * FROM sensor_readings WHERE pondId = :pondId ORDER BY id DESC LIMIT 1")
    fun observeLatestReading(pondId: String): Flow<SensorReadingEntity?>

    @Query("SELECT * FROM sensor_readings WHERE pondId = :pondId ORDER BY id DESC LIMIT :limit")
    suspend fun getRecentReadings(pondId: String, limit: Int): List<SensorReadingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReading(reading: SensorReadingEntity)
}

@Dao
interface AlertDao {
    @Query("SELECT * FROM alerts ORDER BY createdAt DESC")
    fun observeAlerts(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE id = :alertId")
    suspend fun getAlert(alertId: String): AlertEntity?

    @Query("UPDATE alerts SET status = 'ACKNOWLEDGED', acknowledgedAt = :timestamp WHERE id = :alertId")
    suspend fun markAcknowledged(alertId: String, timestamp: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlerts(alerts: List<AlertEntity>)
}
