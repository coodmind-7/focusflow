package com.focusflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_goals")
data class DailyGoalEntity(
    @PrimaryKey val date: String,
    val goalSeconds: Long,
    val achievedSeconds: Long,
    val achieved: Boolean
)
