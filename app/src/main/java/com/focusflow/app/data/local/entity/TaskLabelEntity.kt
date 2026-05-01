package com.focusflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.focusflow.app.domain.model.TaskLabel
import com.focusflow.app.domain.model.TimerMode

@Entity(tableName = "task_labels")
data class TaskLabelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: String,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val defaultTimerMode: String = TimerMode.COUNT_UP.name,
    val defaultDurationMinutes: Int? = null
)

fun TaskLabelEntity.toDomain() = TaskLabel(
    id = id,
    name = name,
    color = color,
    sortOrder = sortOrder,
    createdAt = createdAt,
    defaultTimerMode = try { TimerMode.valueOf(defaultTimerMode) } catch (_: Exception) { TimerMode.COUNT_UP },
    defaultDurationMinutes = defaultDurationMinutes
)

fun TaskLabel.toEntity() = TaskLabelEntity(
    id = id,
    name = name,
    color = color,
    sortOrder = sortOrder,
    createdAt = createdAt,
    defaultTimerMode = defaultTimerMode.name,
    defaultDurationMinutes = defaultDurationMinutes
)
