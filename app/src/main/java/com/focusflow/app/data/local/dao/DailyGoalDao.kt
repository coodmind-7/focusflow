package com.focusflow.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.focusflow.app.data.local.entity.DailyGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyGoalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goal: DailyGoalEntity)

    @Query("SELECT * FROM daily_goals WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): DailyGoalEntity?

    @Query("SELECT * FROM daily_goals ORDER BY date DESC")
    fun getAllDesc(): Flow<List<DailyGoalEntity>>

    @Query("SELECT COUNT(*) FROM daily_goals WHERE achieved = 1")
    fun getTotalAchievedCount(): Flow<Int>

    @Query("SELECT * FROM daily_goals WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getByDateRange(startDate: String, endDate: String): Flow<List<DailyGoalEntity>>

    @Query("SELECT * FROM daily_goals ORDER BY date ASC")
    suspend fun getAllGoals(): List<DailyGoalEntity>

    @Query("DELETE FROM daily_goals")
    suspend fun deleteAllGoals()

    @Insert
    suspend fun insertAllGoals(goals: List<DailyGoalEntity>)

    @Transaction
    suspend fun replaceAllGoals(goals: List<DailyGoalEntity>) {
        deleteAllGoals()
        insertAllGoals(goals)
    }
}
