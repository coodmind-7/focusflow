package com.focusflow.app.di

import android.content.Context
import androidx.room.Room
import com.focusflow.app.data.local.FocusFlowDatabase
import com.focusflow.app.data.local.MIGRATION_1_2
import com.focusflow.app.data.local.MIGRATION_2_3
import com.focusflow.app.data.local.MIGRATION_3_4
import com.focusflow.app.data.local.dao.DailyGoalDao
import com.focusflow.app.data.local.dao.TaskLabelDao
import com.focusflow.app.data.local.dao.TimerRecordDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FocusFlowDatabase {
        return Room.databaseBuilder(
            context,
            FocusFlowDatabase::class.java,
            "focusflow.db"
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build()
    }

    @Provides
    fun provideTaskLabelDao(db: FocusFlowDatabase): TaskLabelDao {
        return db.taskLabelDao()
    }

    @Provides
    fun provideTimerRecordDao(db: FocusFlowDatabase): TimerRecordDao {
        return db.timerRecordDao()
    }

    @Provides
    fun provideDailyGoalDao(db: FocusFlowDatabase): DailyGoalDao {
        return db.dailyGoalDao()
    }
}
