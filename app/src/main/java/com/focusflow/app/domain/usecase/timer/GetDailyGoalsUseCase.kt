package com.focusflow.app.domain.usecase.timer

import com.focusflow.app.data.local.dao.DailyGoalDao
import com.focusflow.app.data.local.entity.DailyGoalEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDailyGoalsUseCase @Inject constructor(
    private val dao: DailyGoalDao
) {
    fun getByDateRange(startDate: String, endDate: String): Flow<List<DailyGoalEntity>> =
        dao.getByDateRange(startDate, endDate)

    fun getAllDesc(): Flow<List<DailyGoalEntity>> = dao.getAllDesc()

    fun getTotalAchievedCount(): Flow<Int> = dao.getTotalAchievedCount()
}
