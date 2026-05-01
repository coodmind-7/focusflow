package com.focusflow.app.domain.model

data class TaskTimeSlice(
    val taskId: Long,
    val taskName: String,
    val color: String,
    val seconds: Long
)

data class StatsData(
    val totalSeconds: Long,
    val taskBreakdown: List<TaskTimeSlice>
) {
    companion object {
        val EMPTY = StatsData(0, emptyList())
    }
}
