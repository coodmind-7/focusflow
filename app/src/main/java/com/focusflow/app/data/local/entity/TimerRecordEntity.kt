package com.focusflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.focusflow.app.domain.model.TimerMode
import com.focusflow.app.domain.model.TimerRecord

@Entity(tableName = "timer_records")
data class TimerRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val date: String,
    val startTime: Long,
    val endTime: Long? = null,
    val duration: Long = 0,
    val mode: String = "COUNT_UP",
    val targetDuration: Long = 0,
    val taskName: String = "",
    val taskColor: String = ""
)

fun TimerRecordEntity.toDomain() = TimerRecord(
    id = id,
    taskId = taskId,
    date = date,
    startTime = startTime,
    endTime = endTime,
    duration = duration,
    mode = TimerMode.valueOf(mode),
    targetDuration = targetDuration,
    taskName = taskName,
    taskColor = taskColor
)

fun TimerRecord.toEntity() = TimerRecordEntity(
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
