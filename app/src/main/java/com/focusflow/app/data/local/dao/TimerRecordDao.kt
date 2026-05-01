package com.focusflow.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.focusflow.app.data.local.entity.TimerRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimerRecordDao {

    @Query("SELECT * FROM timer_records WHERE date = :date ORDER BY startTime DESC")
    fun getRecordsByDate(date: String): Flow<List<TimerRecordEntity>>

    @Query("SELECT * FROM timer_records WHERE date BETWEEN :startDate AND :endDate ORDER BY startTime DESC")
    fun getRecordsByDateRange(startDate: String, endDate: String): Flow<List<TimerRecordEntity>>

    @Insert
    suspend fun insertRecord(record: TimerRecordEntity): Long

    @Update
    suspend fun updateRecord(record: TimerRecordEntity)

    @Query("SELECT * FROM timer_records WHERE endTime IS NULL ORDER BY id DESC LIMIT 1")
    suspend fun getRunningRecord(): TimerRecordEntity?

    @Query("SELECT * FROM timer_records WHERE id = :id")
    suspend fun getRecordById(id: Long): TimerRecordEntity?

    @Query("SELECT * FROM timer_records ORDER BY startTime ASC")
    suspend fun getAllRecords(): List<TimerRecordEntity>

    @Query("DELETE FROM timer_records")
    suspend fun deleteAllRecords()

    @Insert
    suspend fun insertAllRecords(records: List<TimerRecordEntity>)

    @Transaction
    suspend fun replaceAllRecords(records: List<TimerRecordEntity>) {
        deleteAllRecords()
        insertAllRecords(records)
    }
}
