package com.focusflow.app.data.backup

import com.focusflow.app.data.local.dao.DailyGoalDao
import com.focusflow.app.data.local.dao.TaskLabelDao
import com.focusflow.app.data.local.dao.TimerRecordDao
import com.focusflow.app.data.local.entity.DailyGoalEntity
import com.focusflow.app.data.local.entity.TimerRecordEntity
import com.focusflow.app.data.local.preferences.UserPreferences
import com.focusflow.app.domain.model.TaskLabel
import com.focusflow.app.domain.model.TimerMode
import com.focusflow.app.domain.model.TimerRecord
import com.focusflow.app.domain.repository.TaskRepository
import com.focusflow.app.domain.repository.TimerRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupManagerTest {

    private val taskRepository: TaskRepository = mockk(relaxed = true)
    private val timerRepository: TimerRepository = mockk(relaxed = true)
    private val taskLabelDao: TaskLabelDao = mockk(relaxed = true)
    private val timerRecordDao: TimerRecordDao = mockk(relaxed = true)
    private val dailyGoalDao: DailyGoalDao = mockk(relaxed = true)
    private val userPreferences: UserPreferences = mockk(relaxed = true)

    private val backupManager = BackupManager(
        taskRepository, timerRepository,
        taskLabelDao, timerRecordDao, dailyGoalDao, userPreferences
    )

    // ── BK-01: Export all data ──

    @Test
    fun `exportToJson exports all categories by default`() = runTest {
        // Setup mocks to return the dailyGoalSeconds flow
        every { userPreferences.dailyGoalSeconds } returns flowOf(28800L)

        coEvery { taskRepository.getAllTasksOnce() } returns listOf(
            TaskLabel(id = 1, name = "Coding", color = "#FF0000", sortOrder = 1,
                createdAt = 1000, defaultTimerMode = TimerMode.COUNT_UP, defaultDurationMinutes = 25)
        )
        coEvery { timerRepository.getAllRecordsOnce() } returns listOf(
            TimerRecord(id = 10, taskId = 1, date = "2024-01-01", startTime = 100,
                endTime = 200, duration = 5000, mode = TimerMode.COUNT_UP, taskName = "Coding", taskColor = "#FF0000")
        )
        coEvery { dailyGoalDao.getAllGoals() } returns listOf(
            DailyGoalEntity(date = "2024-01-01", goalSeconds = 28800, achievedSeconds = 10000, achieved = false)
        )

        val json = backupManager.exportToJson()

        val data = Json.decodeFromString(BackupData.serializer(), json)
        assertEquals(1, data.tasks.size)
        assertEquals("Coding", data.tasks.first().name)
        assertEquals(1, data.timerRecords.size)
        assertEquals(10, data.timerRecords.first().id)
        assertEquals(1, data.dailyGoals.size)
        assertEquals("2024-01-01", data.dailyGoals.first().date)
        assertEquals(28800, data.dailyGoalSeconds)
    }

    // ── BK-02: Selective export ──

    @Test
    fun `exportToJson skips excluded categories`() = runTest {
        every { userPreferences.dailyGoalSeconds } returns flowOf(28800L)
        coEvery { taskRepository.getAllTasksOnce() } returns emptyList()
        coEvery { timerRepository.getAllRecordsOnce() } returns emptyList()
        coEvery { dailyGoalDao.getAllGoals() } returns emptyList()

        val json = backupManager.exportToJson(
            includeTasks = true,
            includeTimerRecords = false,
            includeDailyGoals = false,
            includeSettings = false
        )

        val data = Json.decodeFromString(BackupData.serializer(), json)
        assertTrue(data.timerRecords.isEmpty())
        assertTrue(data.dailyGoals.isEmpty())
    }

    @Test
    fun `exportToJson empty data produces valid JSON`() = runTest {
        every { userPreferences.dailyGoalSeconds } returns flowOf(28800L)
        coEvery { taskRepository.getAllTasksOnce() } returns emptyList()
        coEvery { timerRepository.getAllRecordsOnce() } returns emptyList()
        coEvery { dailyGoalDao.getAllGoals() } returns emptyList()

        val json = backupManager.exportToJson()

        val data = Json.decodeFromString(BackupData.serializer(), json)
        assertEquals(1, data.version)
        assertTrue(data.tasks.isEmpty())
        assertTrue(data.timerRecords.isEmpty())
        assertTrue(data.dailyGoals.isEmpty())
        assertNotNull(data.exportDate)
    }

    // ── BK-03: Import full data with ID remapping ──

    @Test
    fun `importFromJson remaps task IDs`() = runTest {
        coEvery { taskLabelDao.replaceAllTasks(any()) } returns listOf(100L, 200L)

        // Capture records during execution instead of at verify time
        val capturedRecords = mutableListOf<List<TimerRecordEntity>>()
        coEvery { timerRecordDao.replaceAllRecords(capture(capturedRecords)) } returns Unit

        val backupJson = """
        {
            "version": 1,
            "exportDate": "2024-01-01T00:00:00Z",
            "tasks": [
                {"id": 1, "name": "TaskA", "color": "#FF0000", "sortOrder": 0, "createdAt": 1000, "defaultTimerMode": "COUNT_UP", "defaultDurationMinutes": null},
                {"id": 2, "name": "TaskB", "color": "#00FF00", "sortOrder": 1, "createdAt": 2000, "defaultTimerMode": "COUNT_DOWN", "defaultDurationMinutes": 30}
            ],
            "timerRecords": [
                {"id": 10, "taskId": 1, "date": "2024-01-01", "startTime": 100, "endTime": 200, "duration": 500, "mode": "COUNT_UP", "targetDuration": 0, "taskName": "TaskA", "taskColor": "#FF0000"},
                {"id": 11, "taskId": 2, "date": "2024-01-01", "startTime": 300, "endTime": 400, "duration": 300, "mode": "COUNT_DOWN", "targetDuration": 1800000, "taskName": "TaskB", "taskColor": "#00FF00"}
            ],
            "dailyGoals": [
                {"date": "2024-01-01", "goalSeconds": 28800, "achievedSeconds": 8000, "achieved": false}
            ],
            "dailyGoalSeconds": 28800
        }
        """.trimIndent()

        backupManager.importFromJson(backupJson)

        coVerify { taskLabelDao.replaceAllTasks(any()) }

        assertEquals(1, capturedRecords.size)
        val insertedRecords = capturedRecords.first()
        assertEquals(2, insertedRecords.size)
        assertEquals(100L, insertedRecords[0].taskId)
        assertEquals(200L, insertedRecords[1].taskId)
    }

    // ── BK-04: Selective import ──

    @Test
    fun `importFromJson only imports selected categories`() = runTest {
        coEvery { taskLabelDao.replaceAllTasks(any()) } returns listOf(999)

        val backupJson = """
        {
            "version": 1,
            "exportDate": "2024-01-01T00:00:00Z",
            "tasks": [{"id": 1, "name": "OnlyTask", "color": "#FF0000", "sortOrder": 0, "createdAt": 1000, "defaultTimerMode": "COUNT_UP", "defaultDurationMinutes": null}],
            "timerRecords": [],
            "dailyGoals": [],
            "dailyGoalSeconds": 28800
        }
        """.trimIndent()

        backupManager.importFromJson(
            jsonString = backupJson,
            importTasks = true,
            importTimerRecords = false,
            importDailyGoals = false,
            importSettings = false
        )

        coVerify { taskLabelDao.replaceAllTasks(any()) }
        coVerify(exactly = 0) { timerRecordDao.replaceAllRecords(any()) }
        coVerify(exactly = 0) { dailyGoalDao.replaceAllGoals(any()) }
    }

    // ── BK-05: Empty JSON arrays don't crash ──

    @Test
    fun `importFromJson handles empty arrays gracefully`() = runTest {
        val backupJson = """
        {
            "version": 1,
            "exportDate": "2024-01-01T00:00:00Z",
            "tasks": [],
            "timerRecords": [],
            "dailyGoals": [],
            "dailyGoalSeconds": 28800
        }
        """.trimIndent()

        // Should not throw
        backupManager.importFromJson(backupJson)

        // replaceAll not called when arrays are empty
        coVerify(exactly = 0) { taskLabelDao.replaceAllTasks(any()) }
        coVerify(exactly = 0) { timerRecordDao.replaceAllRecords(any()) }
        coVerify(exactly = 0) { dailyGoalDao.replaceAllGoals(any()) }
    }

    // ── BK-06: DailyGoal achieved boolean preserved ──

    @Test
    fun `importFromJson preserves achieved boolean on daily goals`() = runTest {
        val capturedGoals = mutableListOf<List<DailyGoalEntity>>()
        coEvery { dailyGoalDao.replaceAllGoals(capture(capturedGoals)) } returns Unit

        val backupJson = """
        {
            "version": 1,
            "exportDate": "2024-01-01T00:00:00Z",
            "tasks": [],
            "timerRecords": [],
            "dailyGoals": [
                {"date": "2024-01-01", "goalSeconds": 100, "achievedSeconds": 200, "achieved": true},
                {"date": "2024-01-02", "goalSeconds": 100, "achievedSeconds": 50, "achieved": false}
            ],
            "dailyGoalSeconds": 100
        }
        """.trimIndent()

        backupManager.importFromJson(backupJson)

        assertEquals(1, capturedGoals.size)
        val goals = capturedGoals.first()
        assertEquals(2, goals.size)
        assertTrue(goals[0].achieved)
        assertTrue(!goals[1].achieved)
    }

    // ── TimerMode serialization round-trip ──

    @Test
    fun `export and import preserves timer mode`() = runTest {
        every { userPreferences.dailyGoalSeconds } returns flowOf(28800L)

        coEvery { taskRepository.getAllTasksOnce() } returns listOf(
            TaskLabel(id = 1, name = "Countdown Task", color = "#FF0000",
                sortOrder = 0, createdAt = 1000, defaultTimerMode = TimerMode.COUNT_DOWN,
                defaultDurationMinutes = 45)
        )
        coEvery { timerRepository.getAllRecordsOnce() } returns listOf(
            TimerRecord(id = 1, taskId = 1, date = "2024-01-01", startTime = 100,
                endTime = 200, duration = 5000, mode = TimerMode.COUNT_DOWN,
                targetDuration = 2700000, taskName = "Countdown Task", taskColor = "#FF0000")
        )
        coEvery { dailyGoalDao.getAllGoals() } returns emptyList()

        val json = backupManager.exportToJson()
        val data = Json.decodeFromString(BackupData.serializer(), json)

        assertEquals("COUNT_DOWN", data.tasks.first().defaultTimerMode)
        assertEquals("COUNT_DOWN", data.timerRecords.first().mode)
        assertEquals(2700000, data.timerRecords.first().targetDuration)
    }
}
