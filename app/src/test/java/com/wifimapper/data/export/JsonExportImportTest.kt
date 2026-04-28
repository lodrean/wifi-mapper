package com.wifimapper.data.export

import com.wifimapper.domain.model.AccessPoint
import com.wifimapper.domain.model.Measurement
import com.wifimapper.domain.model.Session
import com.wifimapper.domain.model.TrajectoryPoint
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonExportImportTest {

    private val exporter = ExportSessionUseCaseImpl()
    private val importer = ImportSessionUseCaseImpl()

    @Test
    fun `export and import roundtrip preserves data`() = runTest {
        val session = createTestSession()

        val json = exporter(session)
        assertNotNull(json)
        assertTrue(json.isNotEmpty())

        val imported = importer(json)
        assertEquals(session.name, imported.name)
        assertEquals(session.stepLengthMeters, imported.stepLengthMeters, 0.001f)
        assertEquals(session.accessPoints.size, imported.accessPoints.size)
        assertEquals(session.measurements.size, imported.measurements.size)
        assertEquals(session.trajectory.size, imported.trajectory.size)
    }

    @Test
    fun `exported json is valid and contains version`() = runTest {
        val session = createTestSession()
        val json = exporter(session)
        println(json)

        assertTrue("JSON should contain version key", json.contains("\"version\""))
        assertTrue("JSON should contain accessPoints key", json.contains("\"accessPoints\""))
        assertTrue("JSON should contain measurements key", json.contains("\"measurements\""))
        assertTrue("JSON should contain trajectory key", json.contains("\"trajectory\""))
    }

    @Test
    fun `imported session has new id`() = runTest {
        val session = createTestSession()
        val json = exporter(session)
        val imported = importer(json)

        assertNotNull(imported.id)
        assertTrue(imported.id.isNotEmpty())
    }

    private fun createTestSession(): Session = Session(
        id = "original-id-123",
        name = "Test Session",
        createdAt = 1714293600000L,
        updatedAt = 1714297200000L,
        isActive = false,
        stepLengthMeters = 0.75f,
        accessPoints = listOf(
            AccessPoint("aa:bb:cc:dd:ee:ff", "MyWiFi", 2412, "802.11n")
        ),
        measurements = listOf(
            Measurement("m1", "s1", 0.5f, 1.2f, -45, "aa:bb:cc:dd:ee:ff", 1714293601000L)
        ),
        trajectory = listOf(
            TrajectoryPoint("t1", "s1", 0f, 0f, 0f, 1714293600000L, true),
            TrajectoryPoint("t2", "s1", 0.5f, 1.2f, 45f, 1714293601000L, true)
        )
    )
}
