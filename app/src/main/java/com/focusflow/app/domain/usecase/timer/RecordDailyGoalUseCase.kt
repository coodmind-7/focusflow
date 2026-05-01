package com.focusflow.app.domain.usecase.timer

import com.focusflow.app.data.local.dao.DailyGoalDao
import com.focusflow.app.data.local.entity.DailyGoalEntity
import javax.inject.Inject

class RecordDailyGoalUseCase @Inject constructor(
    private val dao: DailyGoalDao
) {
    suspend operator fun invoke(date: String, goalSeconds: Long, achievedSeconds: Long): Boolean {
        val existing = dao.getByDate(date)
        val alreadyAchieved = existing?.achieved == true
        val nowAchieved = achievedSeconds >= goalSeconds
        dao.upsert(
            DailyGoalEntity(
                date = date,
                goalSeconds = goalSeconds,
                achievedSeconds = achievedSeconds,
                achieved = alreadyAchieved || nowAchieved
            )
        )
        return !alreadyAchieved && nowAchieved
    }
}
