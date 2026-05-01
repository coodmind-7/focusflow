package com.focusflow.app.data.local.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.focusflow.app.data.local.FocusFlowDatabase
import com.focusflow.app.data.local.entity.DailyGoalEntity
import com.focusflow.app.data.local.entity.TaskLabelEntity
import com.focusflow.app.data.local.entity.TimerRecordEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DaoTest {

    private lateinit var db: FocusFlowDatabase
    private lateinit var taskDao: TaskLabelDao
    private lateinit var timerDao: TimerRecordDao
    private lateinit var goalDao: DailyGoalDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            FocusFlowDatabase::class.java
        ).build()
        taskDao = db.taskLabelDao()
        timerDao = db.timerRecordDao()
        goalDao = db.dailyGoalDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ── TaskLabelDao ──

    @Test
    fun `insert_and_query_tasks_returns_in_sortOrder`() = runTest {
        taskDao.insertTask(TaskLabelEntity(name = "B", color = "#FF0000", sortOrder = 1))
        taskDao.insertTask(TaskLabelEntity(name = "A", color = "#00FF00", sortOrder = 0))
        taskDao.insertTask(TaskLabelEntity(name = "C", color = "#0000FF", sortOrder = 2))

        val tasks = taskDao.getAllTasks().first()

        assertEquals(3, tasks.size)
        assertEquals("A", tasks[0].name)
        assertEquals("B", tasks[1].name)
        assertEquals("C", tasks[2].name)
    }

    @Test
    fun `update_task_changes_name_and_color`() = runTest {
        val id = taskDao.insertTask(TaskLabelEntity(name = "Original", color = "#FF0000", sortOrder = 0))

        taskDao.updateTask(TaskLabelEntity(id = id, name = "Updated", color = "#00FF00", sortOrder = 0))

        val tasks = taskDao.getAllTasks().first()
        assertEquals("Updated", tasks.first().name)
        assertEquals("#00FF00", tasks.first().color)
    }

    @Test
    fun `delete_task_removes_from_database`() = runTest {
        val id = taskDao.insertTask(TaskLabelEntity(name = "ToDelete", color = "#FF0000", sortOrder = 0))

        taskDao.deleteTask(TaskLabelEntity(id = id, name = "ToDelete", color = "#FF0000"))

        val tasks = taskDao.getAllTasks().first()
        assertTrue(tasks.isEmpty())
    }

    @Test
    fun `reorderTasks_updates_all_sort_orders`() = runTest {
        val id1 = taskDao.insertTask(TaskLabelEntity(name = "A", color = "#FF0000", sortOrder = 0))
        val id2 = taskDao.insertTask(TaskLabelEntity(name = "B", color = "#00FF00", sortOrder = 1))

        taskDao.reorderTasks(listOf(id1 to 1, id2 to 0))

        val tasks = taskDao.getAllTasks().first()
        // getAllTasks() returns ORDER BY sortOrder ASC
        assertEquals(0, tasks[0].sortOrder)
        assertEquals(1, tasks[1].sortOrder)
    }

    @Test
    fun `getMaxSortOrder_returns_max_value`() = runTest {
        taskDao.insertTask(TaskLabelEntity(name = "A", color = "#FF0000", sortOrder = 3))
        taskDao.insertTask(TaskLabelEntity(name = "B", color = "#00FF00", sortOrder = 7))

        val max = taskDao.getMaxSortOrder()

        assertEquals(7, max)
    }

    @Test
    fun `getAllTasksSuspend_returns_all_tasks`() = runTest {
        taskDao.insertTask(TaskLabelEntity(name = "X", color = "#FF0000", sortOrder = 0))
        taskDao.insertTask(TaskLabelEntity(name = "Y", color = "#00FF00", sortOrder = 1))

        val result = taskDao.getAllTasksSuspend()

        assertEquals(2, result.size)
    }

    @Test
    fun `deleteAllTasks_clears_all_tasks`() = runTest {
        taskDao.insertTask(TaskLabelEntity(name = "A", color = "#FF0000", sortOrder = 0))
        taskDao.insertTask(TaskLabelEntity(name = "B", color = "#00FF00", sortOrder = 1))

        taskDao.deleteAllTasks()

        val result = taskDao.getAllTasksSuspend()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `insertAllTasks_bulk_inserts`() = runTest {
        val tasks = listOf(
            TaskLabelEntity(name = "Batch1", color = "#AA0000", sortOrder = 0),
            TaskLabelEntity(name = "Batch2", color = "#00AA00", sortOrder = 1)
        )

        taskDao.insertAllTasks(tasks)

        val result = taskDao.getAllTasksSuspend()
        assertEquals(2, result.size)
    }

    // ── TimerRecordDao ──

    @Test
    fun `insert_and_query_timer_records_by_date`() = runTest {
        timerDao.insertRecord(TimerRecordEntity(
            taskId = 1, date = "2024-01-01", startTime = 100,
            endTime = 200, duration = 100, mode = "COUNT_UP"
        ))
        timerDao.insertRecord(TimerRecordEntity(
            taskId = 2, date = "2024-01-02", startTime = 300,
            endTime = 400, duration = 200, mode = "COUNT_DOWN"
        ))

        val day1 = timerDao.getRecordsByDate("2024-01-01").first()
        val day2 = timerDao.getRecordsByDate("2024-01-02").first()

        assertEquals(1, day1.size)
        assertEquals(1, day2.size)
        assertEquals(1, day1.first().taskId)
        assertEquals(2, day2.first().taskId)
    }

    @Test
    fun `getRecordsByDateRange_filters_correctly`() = runTest {
        timerDao.insertRecord(TimerRecordEntity(taskId = 1, date = "2024-01-01", startTime = 100,
            duration = 100, mode = "COUNT_UP"))
        timerDao.insertRecord(TimerRecordEntity(taskId = 2, date = "2024-01-05", startTime = 200,
            duration = 200, mode = "COUNT_UP"))
        timerDao.insertRecord(TimerRecordEntity(taskId = 3, date = "2024-01-10", startTime = 300,
            duration = 300, mode = "COUNT_UP"))

        val range = timerDao.getRecordsByDateRange("2024-01-01", "2024-01-05").first()

        assertEquals(2, range.size)
    }

    @Test
    fun `getRunningRecord_returns_record_with_null_endTime`() = runTest {
        timerDao.insertRecord(TimerRecordEntity(
            taskId = 1, date = "2024-01-01", startTime = 100,
            endTime = null, duration = 0, mode = "COUNT_UP"
        ))
        timerDao.insertRecord(TimerRecordEntity(
            taskId = 2, date = "2024-01-01", startTime = 200,
            endTime = 300, duration = 100, mode = "COUNT_UP"
        ))

        val running = timerDao.getRunningRecord()

        assertNotNull(running)
        assertEquals(1, running!!.taskId)
        assertEquals(null, running.endTime)
    }

    @Test
    fun `updateRecord_changes_duration_and_endTime`() = runTest {
        val id = timerDao.insertRecord(TimerRecordEntity(
            taskId = 1, date = "2024-01-01", startTime = 100,
            endTime = null, duration = 0, mode = "COUNT_UP"
        ))

        timerDao.updateRecord(TimerRecordEntity(
            id = id, taskId = 1, date = "2024-01-01", startTime = 100,
            endTime = 500, duration = 400, mode = "COUNT_UP"
        ))

        val updated = timerDao.getRecordById(id)
        assertNotNull(updated)
        assertEquals(500L, updated!!.endTime)
        assertEquals(400L, updated.duration)
    }

    @Test
    fun `getRecordById_returns_null_for_non-existent_id`() = runTest {
        val record = timerDao.getRecordById(999)

        assertNull(record)
    }

    @Test
    fun `getAllRecords_returns_all_records_sorted_by_startTime_ASC`() = runTest {
        timerDao.insertRecord(TimerRecordEntity(taskId = 3, date = "2024-01-01", startTime = 300,
            duration = 100, mode = "COUNT_UP"))
        timerDao.insertRecord(TimerRecordEntity(taskId = 1, date = "2024-01-01", startTime = 100,
            duration = 100, mode = "COUNT_UP"))
        timerDao.insertRecord(TimerRecordEntity(taskId = 2, date = "2024-01-01", startTime = 200,
            duration = 100, mode = "COUNT_UP"))

        val all = timerDao.getAllRecords()

        assertEquals(3, all.size)
        assertEquals(100, all[0].startTime)
        assertEquals(200, all[1].startTime)
        assertEquals(300, all[2].startTime)
    }

    @Test
    fun `deleteAllRecords_clears_all_records`() = runTest {
        timerDao.insertRecord(TimerRecordEntity(taskId = 1, date = "2024-01-01", startTime = 100,
            duration = 100, mode = "COUNT_UP"))

        timerDao.deleteAllRecords()

        val all = timerDao.getAllRecords()
        assertTrue(all.isEmpty())
    }

    @Test
    fun `insertAllRecords_bulk_inserts`() = runTest {
        val records = listOf(
            TimerRecordEntity(taskId = 1, date = "2024-01-01", startTime = 100, duration = 100, mode = "COUNT_UP"),
            TimerRecordEntity(taskId = 2, date = "2024-01-01", startTime = 200, duration = 200, mode = "COUNT_DOWN")
        )

        timerDao.insertAllRecords(records)

        val all = timerDao.getAllRecords()
        assertEquals(2, all.size)
    }

    // ── DailyGoalDao ──

    @Test
    fun `upsert_inserts_new_goal`() = runTest {
        goalDao.upsert(DailyGoalEntity(date = "2024-01-01", goalSeconds = 3600,
            achievedSeconds = 1000, achieved = false))

        val goal = goalDao.getByDate("2024-01-01")

        assertNotNull(goal)
        assertEquals(3600, goal!!.goalSeconds)
        assertEquals(1000, goal.achievedSeconds)
        assertEquals(false, goal.achieved)
    }

    @Test
    fun `upsert_replaces_existing_goal_on_same_date`() = runTest {
        goalDao.upsert(DailyGoalEntity(date = "2024-01-01", goalSeconds = 3600,
            achievedSeconds = 1000, achieved = false))
        goalDao.upsert(DailyGoalEntity(date = "2024-01-01", goalSeconds = 3600,
            achievedSeconds = 4000, achieved = true))

        val goal = goalDao.getByDate("2024-01-01")

        assertEquals(4000, goal!!.achievedSeconds)
        assertEquals(true, goal.achieved)
    }

    @Test
    fun `getByDateRange_filters_goals_by_date_range`() = runTest {
        goalDao.upsert(DailyGoalEntity("2024-01-01", 3600, 1000, false))
        goalDao.upsert(DailyGoalEntity("2024-01-05", 3600, 2000, false))
        goalDao.upsert(DailyGoalEntity("2024-01-10", 3600, 3000, true))

        val range = goalDao.getByDateRange("2024-01-01", "2024-01-05").first()

        assertEquals(2, range.size)
    }

    @Test
    fun `getTotalAchievedCount_returns_correct_count`() = runTest {
        goalDao.upsert(DailyGoalEntity("2024-01-01", 3600, 4000, true))
        goalDao.upsert(DailyGoalEntity("2024-01-02", 3600, 2000, false))
        goalDao.upsert(DailyGoalEntity("2024-01-03", 3600, 5000, true))

        val count = goalDao.getTotalAchievedCount().first()

        assertEquals(2, count)
    }

    @Test
    fun `getAllGoals_returns_all_goals_sorted_ASC_by_date`() = runTest {
        goalDao.upsert(DailyGoalEntity("2024-01-03", 3600, 1000, false))
        goalDao.upsert(DailyGoalEntity("2024-01-01", 3600, 2000, false))

        val all = goalDao.getAllGoals()

        assertEquals(2, all.size)
        assertEquals("2024-01-01", all[0].date)
        assertEquals("2024-01-03", all[1].date)
    }

    @Test
    fun `getAllDesc_returns_goals_sorted_DESC_by_date`() = runTest {
        goalDao.upsert(DailyGoalEntity("2024-01-01", 3600, 1000, false))
        goalDao.upsert(DailyGoalEntity("2024-01-03", 3600, 2000, false))

        val all = goalDao.getAllDesc().first()

        assertEquals("2024-01-03", all[0].date)
        assertEquals("2024-01-01", all[1].date)
    }

    @Test
    fun `deleteAllGoals_clears_all_goals`() = runTest {
        goalDao.upsert(DailyGoalEntity("2024-01-01", 3600, 1000, false))

        goalDao.deleteAllGoals()

        val all = goalDao.getAllGoals()
        assertTrue(all.isEmpty())
    }

    @Test
    fun `insertAllGoals_bulk_inserts`() = runTest {
        val goals = listOf(
            DailyGoalEntity("2024-01-01", 3600, 1000, false),
            DailyGoalEntity("2024-01-02", 3600, 2000, true)
        )

        goalDao.insertAllGoals(goals)

        val all = goalDao.getAllGoals()
        assertEquals(2, all.size)
    }
}
