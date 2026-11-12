package software.royya.watch.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ponds")
data class PondEntity(
    @PrimaryKey val id: String,
    val name: String,
    val location: String,
    val deviceId: String,
    val connectivity: String,
    val healthStatus: String,
    val healthScore: Int?,
    val lastUpdated: String
)

@Entity(tableName = "sensor_readings")
data class SensorReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pondId: String,
    val temperature: Float?,
    val turbidity: Float?,
    val tds: Float?,
    val ph: Float?,
    val dissolvedOxygen: Float?,
    val oxygenSource: String,
    val timestamp: String
)

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey val id: String,
    val pondId: String,
    val pondName: String,
    val severity: String,
    val type: String,
    val title: String,
    val message: String,
    val parameter: String?,
    val value: String?,
    val recommendation: String?,
    val language: String,
    val audioReference: String?,
    val createdAt: String,
    val acknowledgedAt: String?,
    val resolvedAt: String?,
    val status: String
)
