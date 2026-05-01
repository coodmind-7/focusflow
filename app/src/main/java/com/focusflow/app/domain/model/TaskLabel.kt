package com.focusflow.app.domain.model

data class TaskLabel(
    val id: Long = 0,
    val name: String,
    val color: String,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val defaultTimerMode: TimerMode = TimerMode.COUNT_UP,
    val defaultDurationMinutes: Int? = null
)
