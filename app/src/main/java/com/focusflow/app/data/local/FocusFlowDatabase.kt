package com.focusflow.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.focusflow.app.data.local.converter.Converters
import com.focusflow.app.data.local.dao.DailyGoalDao
import com.focusflow.app.data.local.dao.TaskLabelDao
import com.focusflow.app.data.local.dao.TimerRecordDao
import com.focusflow.app.data.local.entity.DailyGoalEntity
import com.focusflow.app.data.local.entity.TaskLabelEntity
import com.focusflow.app.data.local.entity.TimerRecordEntity

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE task_labels ADD COLUMN defaultTimerMode TEXT NOT NULL DEFAULT 'COUNT_UP'")
        db.execSQL("ALTER TABLE task_labels ADD COLUMN defaultDurationMinutes INTEGER DEFAULT NULL")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS daily_goals (
                date TEXT NOT NULL PRIMARY KEY,
                goalSeconds INTEGER NOT NULL,
                achievedSeconds INTEGER NOT NULL,
                achieved INTEGER NOT NULL DEFAULT 0
            )
        """)
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE timer_records ADD COLUMN taskName TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE timer_records ADD COLUMN taskColor TEXT NOT NULL DEFAULT ''")
    }
}

@Database(
    entities = [TaskLabelEntity::class, TimerRecordEntity::class, DailyGoalEntity::class],
    version = 4,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class FocusFlowDatabase : RoomDatabase() {
    abstract fun taskLabelDao(): TaskLabelDao
    abstract fun timerRecordDao(): TimerRecordDao
    abstract fun dailyGoalDao(): DailyGoalDao
}
