package com.focusflow.app.domain.usecase.timer

import com.focusflow.app.domain.model.TimerRecord
import com.focusflow.app.domain.repository.TimerRepository
import javax.inject.Inject

class StartTimerUseCase @Inject constructor(
    private val repository: TimerRepository
) {
    suspend operator fun invoke(record: TimerRecord): Long {
        return repository.insertRecord(record)
    }
}
