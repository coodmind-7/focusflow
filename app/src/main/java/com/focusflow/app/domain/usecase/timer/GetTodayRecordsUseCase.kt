package com.focusflow.app.domain.usecase.timer

import com.focusflow.app.domain.model.TimerRecord
import com.focusflow.app.domain.repository.TimerRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTodayRecordsUseCase @Inject constructor(
    private val repository: TimerRepository
) {
    operator fun invoke(date: String): Flow<List<TimerRecord>> {
        return repository.getRecordsByDate(date)
    }
}
