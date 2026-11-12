package software.royya.watch.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import software.royya.watch.data.local.dao.AlertDao
import software.royya.watch.data.local.dao.PondDao
import software.royya.watch.data.local.dao.SensorReadingDao
import software.royya.watch.data.local.entity.AlertEntity
import software.royya.watch.data.local.entity.PondEntity
import software.royya.watch.data.local.entity.SensorReadingEntity

@Database(
    entities = [
        PondEntity::class,
        SensorReadingEntity::class,
        AlertEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class RoyyaDatabase : RoomDatabase() {
    abstract fun pondDao(): PondDao
    abstract fun sensorReadingDao(): SensorReadingDao
    abstract fun alertDao(): AlertDao
}
