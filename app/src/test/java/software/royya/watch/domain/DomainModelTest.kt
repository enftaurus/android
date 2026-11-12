package software.royya.watch.domain

import org.junit.Assert.*
import org.junit.Test
import software.royya.watch.domain.model.*

class DomainModelTest {

    @Test
    fun sensorReading_preservesNullForUnknownMeasurements_neverZero() {
        val reading = SensorReading(
            pondId = "pond_01",
            temperature = 28.5f,
            turbidity = 2.1f,
            tds = null,
            ph = null,
            dissolvedOxygen = 5.8f,
            oxygenSource = OxygenSource.PREDICTED,
            timestamp = "2026-11-10T00:00:00Z"
        )

        assertNotNull(reading.temperature)
        assertNotNull(reading.dissolvedOxygen)
        assertNull(reading.tds)
        assertNull(reading.ph)
        assertEquals(OxygenSource.PREDICTED, reading.oxygenSource)
    }

    @Test
    fun alertModel_initialState_isNew() {
        val alert = Alert(
            id = "alert_1",
            pondId = "pond_01",
            severity = AlertSeverity.CRITICAL,
            type = AlertType.LOW_DISSOLVED_OXYGEN,
            title = "అత్యవసర హెచ్చరిక",
            message = "ఆక్సిజన్ తక్కువగా ఉంది",
            createdAt = "2026-11-10T00:00:00Z",
            status = AlertStatus.NEW
        )

        assertEquals(AlertStatus.NEW, alert.status)
        assertNull(alert.acknowledgedAt)
        assertNull(alert.resolvedAt)
    }
}
