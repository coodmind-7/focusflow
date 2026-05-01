package com.focusflow.app.data.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val version: Int = 1,
    val exportDate: String = "",
    val tasks: List<TaskBackupItem> = emptyList(),
    val timerRecords: List<TimerRecordBackupItem> = emptyList(),
    val dailyGoals: List<DailyGoalBackupItem> = emptyList(),
    val dailyGoalSeconds: Long = 28800
)

@Serializable
data class TaskBackupItem(
    val id: Long,
    val name: String,
    val color: String,
    val sortOrder: Int,
    val createdAt: Long,
    val defaultTimerMode: String,
    val defaultDurationMinutes: Int? = null
)

@Serializable
data class TimerRecordBackupItem(
    val id: Long,
    val taskId: Long,
    val date: String,
    val startTime: Long,
    val endTime: Long? = null,
    val duration: Long = 0,
    val mode: String,
    val targetDuration: Long = 0,
    val taskName: String = "",
    val taskColor: String = ""
)

@Serializable
data class DailyGoalBackupItem(
    val date: String,
    val goalSeconds: Long,
    val achievedSeconds: Long,
    val achieved: Boolean
)
