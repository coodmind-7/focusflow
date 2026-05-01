package com.focusflow.app.data.local.entity

import com.focusflow.app.domain.model.TaskLabel
import com.focusflow.app.domain.model.TimerMode
import com.focusflow.app.domain.model.TimerRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EntityMapperTest {

    // ── TaskLabelEntity ↔ TaskLabel ──

    @Test
    fun `TaskLabelEntity toDomain maps all fields correctly`() {
        val entity = TaskLabelEntity(
            id = 42,
            name = "Coding",
            color = "#FF5733",
            sortOrder = 3,
            createdAt = 1700000000000,
            defaultTimerMode = "COUNT_DOWN",
            defaultDurationMinutes = 25
        )

        val domain = entity.toDomain()

        assertEquals(42, domain.id)
        assertEquals("Coding", domain.name)
        assertEquals("#FF5733", domain.color)
        assertEquals(3, domain.sortOrder)
        assertEquals(1700000000000, domain.createdAt)
        assertEquals(TimerMode.COUNT_DOWN, domain.defaultTimerMode)
        assertEquals(25, domain.defaultDurationMinutes)
    }

    @Test
    fun `TaskLabelEntity toDomain handles invalid timer mode gracefully`() {
        val entity = TaskLabelEntity(
            id = 1, name = "Bad", color = "#000000",
            sortOrder = 0, createdAt = 0,
            defaultTimerMode = "INVALID_MODE",
            defaultDurationMinutes = null
        )

        val domain = entity.toDomain()

        assertEquals("Should fall back to COUNT_UP for invalid mode string", TimerMode.COUNT_UP, domain.defaultTimerMode)
    }

    @Test
    fun `TaskLabelEntity toDomain handles null duration minutes`() {
        val entity = TaskLabelEntity(
            id = 1, name = "No Duration", color = "#000000",
            sortOrder = 0, createdAt = 0,
            defaultTimerMode = "COUNT_UP",
            defaultDurationMinutes = null
        )

        val domain = entity.toDomain()

        assertNull(domain.defaultDurationMinutes)
    }

    @Test
    fun `TaskLabel toEntity maps all fields correctly`() {
        val domain = TaskLabel(
            id = 99,
            name = "Reading",
            color = "#33FF57",
            sortOrder = 5,
            createdAt = 1700000000000,
            defaultTimerMode = TimerMode.COUNT_DOWN,
            defaultDurationMinutes = 45
        )

        val entity = domain.toEntity()

        assertEquals(99, entity.id)
        assertEquals("Reading", entity.name)
        assertEquals("#33FF57", entity.color)
        assertEquals(5, entity.sortOrder)
        assertEquals(1700000000000, entity.createdAt)
        assertEquals("COUNT_DOWN", entity.defaultTimerMode)
        assertEquals(45, entity.defaultDurationMinutes)
    }

    @Test
    fun `TaskLabel toEntity with null durationMinutes`() {
        val domain = TaskLabel(
            id = 1, name = "Flex", color = "#000000",
            sortOrder = 0, createdAt = 0,
            defaultTimerMode = TimerMode.COUNT_UP,
            defaultDurationMinutes = null
        )

        val entity = domain.toEntity()

        assertNull(entity.defaultDurationMinutes)
    }

    // ── TimerRecordEntity ↔ TimerRecord ──

    @Test
    fun `TimerRecordEntity toDomain maps all fields`() {
        val entity = TimerRecordEntity(
            id = 7,
            taskId = 3,
            date = "2024-01-15",
            startTime = 1000,
            endTime = 5000,
            duration = 4000,
            mode = "COUNT_DOWN",
            targetDuration = 1800000,
            taskName = "Focus",
            taskColor = "#FF0000"
        )

        val domain = entity.toDomain()

        assertEquals(7, domain.id)
        assertEquals(3, domain.taskId)
        assertEquals("2024-01-15", domain.date)
        assertEquals(1000, domain.startTime)
        assertEquals(5000L, domain.endTime)
        assertEquals(4000L, domain.duration)
        assertEquals(TimerMode.COUNT_DOWN, domain.mode)
        assertEquals(1800000L, domain.targetDuration)
        assertEquals("Focus", domain.taskName)
        assertEquals("#FF0000", domain.taskColor)
    }

    @Test
    fun `TimerRecordEntity toDomain with null endTime`() {
        val entity = TimerRecordEntity(
            id = 1, taskId = 1, date = "2024-01-01",
            startTime = 100, endTime = null, duration = 0,
            mode = "COUNT_UP"
        )

        val domain = entity.toDomain()

        assertNull(domain.endTime)
    }

    @Test
    fun `TimerRecord toEntity maps all fields`() {
        val domain = TimerRecord(
            id = 10,
            taskId = 5,
            date = "2024-01-20",
            startTime = 2000,
            endTime = 6000,
            duration = 4000,
            mode = TimerMode.COUNT_UP,
            targetDuration = 0,
            taskName = "Study",
            taskColor = "#00FF00"
        )

        val entity = domain.toEntity()

        assertEquals(10, entity.id)
        assertEquals(5, entity.taskId)
        assertEquals("Study", entity.taskName)
        assertEquals("#00FF00", entity.taskColor)
        assertEquals("COUNT_UP", entity.mode)
    }

    // ── Round-trip: no data loss ──

    @Test
    fun `TaskLabel entity domain round trip`() {
        val original = TaskLabel(
            id = 5, name = "RoundTrip", color = "#AABBCC",
            sortOrder = 2, createdAt = 123456789,
            defaultTimerMode = TimerMode.COUNT_DOWN, defaultDurationMinutes = 30
        )

        val roundTripped = original.toEntity().toDomain()

        assertEquals(original, roundTripped)
    }

    @Test
    fun `TimerRecord entity domain round trip`() {
        val original = TimerRecord(
            id = 8, taskId = 2, date = "2024-01-01",
            startTime = 100, endTime = 500, duration = 400,
            mode = TimerMode.COUNT_DOWN, targetDuration = 3600000,
            taskName = "Test", taskColor = "#123456"
        )

        val roundTripped = original.toEntity().toDomain()

        assertEquals(original, roundTripped)
    }
}
