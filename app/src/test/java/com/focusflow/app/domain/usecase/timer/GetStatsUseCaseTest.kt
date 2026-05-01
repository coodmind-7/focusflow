package com.focusflow.app.domain.usecase.timer

import com.focusflow.app.domain.model.StatsData
import com.focusflow.app.domain.model.TimerMode
import com.focusflow.app.domain.model.TimerRecord
import com.focusflow.app.domain.repository.TimerRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetStatsUseCaseTest {

    private val repository: TimerRepository = mockk()
    private val useCase = GetStatsUseCase(repository)

    // ── UC-01: Group by taskId, use taskName snapshot ──

    @Test
    fun `groups records by taskId and uses taskName snapshot`() = runTest {
        val records = listOf(
            TimerRecord(id = 1, taskId = 10, date = "2024-01-01", startTime = 100,
                endTime = 200, duration = 5000, mode = TimerMode.COUNT_UP,
                taskName = "Coding", taskColor = "#FF0000"),
            TimerRecord(id = 2, taskId = 10, date = "2024-01-01", startTime = 300,
                endTime = 400, duration = 3000, mode = TimerMode.COUNT_UP,
                taskName = "Coding", taskColor = "#FF0000"),
            TimerRecord(id = 3, taskId = 20, date = "2024-01-01", startTime = 500,
                endTime = 600, duration = 10000, mode = TimerMode.COUNT_DOWN,
                taskName = "Reading", taskColor = "#00FF00")
        )
        coEvery { repository.getRecordsByDateRange(any(), any()) } returns flowOf(records)

        val stats = useCase("2024-01-01", "2024-01-01").first()

        assertEquals(2, stats.taskBreakdown.size)
        val codingSlice = stats.taskBreakdown.find { it.taskId == 10L }
        val readingSlice = stats.taskBreakdown.find { it.taskId == 20L }

        assertTrue(codingSlice != null)
        assertEquals("Coding", codingSlice!!.taskName)
        assertEquals("#FF0000", codingSlice.color)
        assertEquals(8, codingSlice.seconds) // (5000 + 3000) / 1000

        assertTrue(readingSlice != null)
        assertEquals("Reading", readingSlice!!.taskName)
        assertEquals("#00FF00", readingSlice.color)
        assertEquals(10, readingSlice.seconds) // 10000 / 1000
    }

    // ── UC-01 (edge): Empty taskName shows fallback ──

    @Test
    fun `empty taskName shows placeholder text`() = runTest {
        val records = listOf(
            TimerRecord(id = 1, taskId = 99, date = "2024-01-01", startTime = 100,
                endTime = 200, duration = 60000, mode = TimerMode.COUNT_UP,
                taskName = "", taskColor = "")
        )
        coEvery { repository.getRecordsByDateRange(any(), any()) } returns flowOf(records)

        val stats = useCase("2024-01-01", "2024-01-01").first()

        val slice = stats.taskBreakdown.first()
        assertEquals("已删除", slice.taskName)
        assertEquals("#CCCCCC", slice.color)
    }

    // ── UC-02: Records with duration <= 0 are filtered out ──

    @Test
    fun `filters out records with zero or negative duration`() = runTest {
        val records = listOf(
            TimerRecord(id = 1, taskId = 10, date = "2024-01-01", startTime = 100,
                endTime = null, duration = 0, mode = TimerMode.COUNT_UP,
                taskName = "Task", taskColor = "#FF0000"),
            TimerRecord(id = 2, taskId = 10, date = "2024-01-01", startTime = 200,
                endTime = 300, duration = 5000, mode = TimerMode.COUNT_UP,
                taskName = "Task", taskColor = "#FF0000")
        )
        coEvery { repository.getRecordsByDateRange(any(), any()) } returns flowOf(records)

        val stats = useCase("2024-01-01", "2024-01-01").first()

        assertEquals(1, stats.taskBreakdown.size)
        assertEquals(5, stats.totalSeconds)
    }

    @Test
    fun `empty records return EMPTY stats`() = runTest {
        coEvery { repository.getRecordsByDateRange(any(), any()) } returns flowOf(emptyList())

        val stats = useCase("2024-01-01", "2024-01-01").first()

        assertEquals(0, stats.totalSeconds)
        assertTrue(stats.taskBreakdown.isEmpty())
    }

    // ── UC-03: Sorted by seconds descending ──

    @Test
    fun `task breakdown sorted by seconds descending`() = runTest {
        val records = listOf(
            TimerRecord(id = 1, taskId = 10, date = "2024-01-01", startTime = 100,
                endTime = 200, duration = 3000, mode = TimerMode.COUNT_UP,
                taskName = "Small", taskColor = "#AAAAAA"),
            TimerRecord(id = 2, taskId = 20, date = "2024-01-01", startTime = 100,
                endTime = 200, duration = 10000, mode = TimerMode.COUNT_UP,
                taskName = "Large", taskColor = "#BBBBBB"),
            TimerRecord(id = 3, taskId = 30, date = "2024-01-01", startTime = 100,
                endTime = 200, duration = 5000, mode = TimerMode.COUNT_UP,
                taskName = "Medium", taskColor = "#CCCCCC")
        )
        coEvery { repository.getRecordsByDateRange(any(), any()) } returns flowOf(records)

        val stats = useCase("2024-01-01", "2024-01-01").first()

        assertEquals(3, stats.taskBreakdown.size)
        assertEquals("Large", stats.taskBreakdown[0].taskName)   // 10s
        assertEquals("Medium", stats.taskBreakdown[1].taskName)  // 5s
        assertEquals("Small", stats.taskBreakdown[2].taskName)   // 3s
    }

    // ── Total seconds calculation ──

    @Test
    fun `totalSeconds is sum of all filtered task seconds`() = runTest {
        val records = listOf(
            TimerRecord(id = 1, taskId = 10, date = "2024-01-01", startTime = 100,
                endTime = 200, duration = 1000, mode = TimerMode.COUNT_UP,
                taskName = "A", taskColor = "#111111"),
            TimerRecord(id = 2, taskId = 20, date = "2024-01-01", startTime = 300,
                endTime = 400, duration = 2000, mode = TimerMode.COUNT_UP,
                taskName = "B", taskColor = "#222222"),
            TimerRecord(id = 3, taskId = 30, date = "2024-01-01", startTime = 500,
                endTime = 600, duration = 500, mode = TimerMode.COUNT_UP,
                taskName = "C", taskColor = "#333333")
        )
        coEvery { repository.getRecordsByDateRange(any(), any()) } returns flowOf(records)

        val stats = useCase("2024-01-01", "2024-01-01").first()

        assertEquals(3, stats.totalSeconds) // (1000+2000+500) / 1000 = 3
    }

    // ── Single task with multiple records ──

    @Test
    fun `single task with multiple records groups correctly`() = runTest {
        val records = (1..5).map { i ->
            TimerRecord(id = i.toLong(), taskId = 1, date = "2024-01-01",
                startTime = i * 100L, endTime = i * 200L, duration = 1000L * i,
                mode = TimerMode.COUNT_UP, taskName = "Consistent", taskColor = "#FF0000")
        }
        coEvery { repository.getRecordsByDateRange(any(), any()) } returns flowOf(records)

        val stats = useCase("2024-01-01", "2024-01-01").first()

        assertEquals(1, stats.taskBreakdown.size)
        val slice = stats.taskBreakdown.first()
        assertEquals("Consistent", slice.taskName)
        assertEquals(15, slice.seconds) // (1+2+3+4+5) * 1000 / 1000
    }
}
