package com.focusflow.app.domain.model

data class TimerRecord(
    val id: Long = 0,
    val taskId: Long,
    val date: String,
    val startTime: Long,
    val endTime: Long? = null,
    val duration: Long = 0,
    val mode: TimerMode = TimerMode.COUNT_UP,
    val targetDuration: Long = 0,
    val taskName: String = "",
    val taskColor: String = ""
)
