package com.focusflow.app.data.backup

import com.focusflow.app.data.local.dao.DailyGoalDao
import com.focusflow.app.data.local.dao.TaskLabelDao
import com.focusflow.app.data.local.dao.TimerRecordDao
import com.focusflow.app.data.local.entity.DailyGoalEntity
import com.focusflow.app.data.local.entity.TaskLabelEntity
import com.focusflow.app.data.local.entity.TimerRecordEntity
import com.focusflow.app.data.local.preferences.UserPreferences
import com.focusflow.app.domain.repository.TaskRepository
import com.focusflow.app.domain.repository.TimerRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    private val taskRepository: TaskRepository,
    private val timerRepository: TimerRepository,
    private val taskLabelDao: TaskLabelDao,
    private val timerRecordDao: TimerRecordDao,
    private val dailyGoalDao: DailyGoalDao,
    private val userPreferences: UserPreferences
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    suspend fun exportToJson(
        includeTasks: Boolean = true,
        includeTimerRecords: Boolean = true,
        includeDailyGoals: Boolean = true,
        includeSettings: Boolean = true
    ): String {
        val tasks = if (includeTasks) {
            taskRepository.getAllTasksOnce().map { it.toBackupItem() }
        } else emptyList()

        val records = if (includeTimerRecords) {
            timerRepository.getAllRecordsOnce().map { it.toBackupItem() }
        } else emptyList()

        val goals = if (includeDailyGoals) {
            dailyGoalDao.getAllGoals().map { it.toBackupItem() }
        } else emptyList()

        val goalSeconds = if (includeSettings) {
            userPreferences.dailyGoalSeconds.first()
        } else DEFAULT_GOAL_SECONDS

        val backupData = BackupData(
            version = 1,
            exportDate = Instant.now().toString(),
            tasks = tasks,
            timerRecords = records,
            dailyGoals = goals,
            dailyGoalSeconds = goalSeconds
        )
        return json.encodeToString(BackupData.serializer(), backupData)
    }

    suspend fun importFromJson(
        jsonString: String,
        importTasks: Boolean = true,
        importTimerRecords: Boolean = true,
        importDailyGoals: Boolean = true,
        importSettings: Boolean = true
    ) {
        val data = json.decodeFromString(BackupData.serializer(), jsonString)

        var taskIdMap: Map<Long, Long> = emptyMap()

        if (importTasks && data.tasks.isNotEmpty()) {
            val entities = data.tasks.map { item ->
                TaskLabelEntity(
                    name = item.name,
                    color = item.color,
                    sortOrder = item.sortOrder,
                    createdAt = item.createdAt,
                    defaultTimerMode = item.defaultTimerMode,
                    defaultDurationMinutes = item.defaultDurationMinutes
                )
            }
            val newIds = taskLabelDao.replaceAllTasks(entities)
            taskIdMap = data.tasks.mapIndexed { i, item -> item.id to newIds[i] }.toMap()
        }

        if (importTimerRecords && data.timerRecords.isNotEmpty()) {
            timerRecordDao.replaceAllRecords(
                data.timerRecords.map { item ->
                    TimerRecordEntity(
                        taskId = taskIdMap[item.taskId] ?: item.taskId,
                        date = item.date,
                        startTime = item.startTime,
                        endTime = item.endTime,
                        duration = item.duration,
                        mode = item.mode,
                        targetDuration = item.targetDuration,
                        taskName = item.taskName,
                        taskColor = item.taskColor
                    )
                }
            )
        }

        if (importDailyGoals && data.dailyGoals.isNotEmpty()) {
            dailyGoalDao.replaceAllGoals(
                data.dailyGoals.map {
                    DailyGoalEntity(
                        date = it.date,
                        goalSeconds = it.goalSeconds,
                        achievedSeconds = it.achievedSeconds,
                        achieved = it.achieved
                    )
                }
            )
        }

        if (importSettings) {
            userPreferences.setDailyGoalSeconds(data.dailyGoalSeconds)
        }
    }

    companion object {
        private const val DEFAULT_GOAL_SECONDS = 8 * 3600L
    }
}

private fun com.focusflow.app.domain.model.TaskLabel.toBackupItem() = TaskBackupItem(
    id = id,
    name = name,
    color = color,
    sortOrder = sortOrder,
    createdAt = createdAt,
    defaultTimerMode = defaultTimerMode.name,
    defaultDurationMinutes = defaultDurationMinutes
)

private fun com.focusflow.app.domain.model.TimerRecord.toBackupItem() = TimerRecordBackupItem(
    id = id,
    taskId = taskId,
    date = date,
    startTime = startTime,
    endTime = endTime,
    duration = duration,
    mode = mode.name,
    targetDuration = targetDuration,
    taskName = taskName,
    taskColor = taskColor
)

private fun DailyGoalEntity.toBackupItem() = DailyGoalBackupItem(
    date = date,
    goalSeconds = goalSeconds,
    achievedSeconds = achievedSeconds,
    achieved = achieved
)
