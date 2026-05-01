package com.focusflow.app.domain.usecase.timer

import com.focusflow.app.domain.repository.TimerRepository
import javax.inject.Inject

class StopTimerUseCase @Inject constructor(
    private val repository: TimerRepository
) {
    suspend operator fun invoke(recordId: Long, endTime: Long, duration: Long) {
        val record = repository.getRecordById(recordId) ?: return
        repository.updateRecord(record.copy(endTime = endTime, duration = duration))
    }

    suspend fun updateDuration(recordId: Long, duration: Long) {
        val record = repository.getRecordById(recordId) ?: return
        repository.updateRecord(record.copy(duration = duration))
    }
}
