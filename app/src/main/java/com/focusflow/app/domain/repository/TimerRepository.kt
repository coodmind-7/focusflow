package com.focusflow.app.domain.repository

import com.focusflow.app.domain.model.TimerRecord
import kotlinx.coroutines.flow.Flow

interface TimerRepository {
    fun getRecordsByDate(date: String): Flow<List<TimerRecord>>
    fun getRecordsByDateRange(startDate: String, endDate: String): Flow<List<TimerRecord>>
    suspend fun insertRecord(record: TimerRecord): Long
    suspend fun updateRecord(record: TimerRecord)
    suspend fun getRunningRecord(): TimerRecord?
    suspend fun getRecordById(id: Long): TimerRecord?
    suspend fun getAllRecordsOnce(): List<TimerRecord>
    suspend fun deleteAllRecords()
    suspend fun insertAllRecords(records: List<TimerRecord>)
}
