package com.focusflow.app.data.repository

import com.focusflow.app.data.local.dao.TimerRecordDao
import com.focusflow.app.data.local.entity.toDomain
import com.focusflow.app.data.local.entity.toEntity
import com.focusflow.app.domain.model.TimerRecord
import com.focusflow.app.domain.repository.TimerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TimerRepositoryImpl @Inject constructor(
    private val dao: TimerRecordDao
) : TimerRepository {

    override fun getRecordsByDate(date: String): Flow<List<TimerRecord>> =
        dao.getRecordsByDate(date).map { entities -> entities.map { it.toDomain() } }

    override fun getRecordsByDateRange(startDate: String, endDate: String): Flow<List<TimerRecord>> =
        dao.getRecordsByDateRange(startDate, endDate).map { entities -> entities.map { it.toDomain() } }

    override suspend fun insertRecord(record: TimerRecord): Long =
        dao.insertRecord(record.toEntity())

    override suspend fun updateRecord(record: TimerRecord) =
        dao.updateRecord(record.toEntity())

    override suspend fun getRunningRecord(): TimerRecord? =
        dao.getRunningRecord()?.toDomain()

    override suspend fun getRecordById(id: Long): TimerRecord? =
        dao.getRecordById(id)?.toDomain()

    override suspend fun getAllRecordsOnce(): List<TimerRecord> =
        dao.getAllRecords().map { it.toDomain() }

    override suspend fun deleteAllRecords() = dao.deleteAllRecords()

    override suspend fun insertAllRecords(records: List<TimerRecord>) =
        dao.insertAllRecords(records.map { it.toEntity() })
}
