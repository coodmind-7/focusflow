package com.focusflow.app.domain.usecase.timer

import com.focusflow.app.domain.model.StatsData
import com.focusflow.app.domain.model.TaskTimeSlice
import com.focusflow.app.domain.repository.TimerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetStatsUseCase @Inject constructor(
    private val timerRepository: TimerRepository
) {
    operator fun invoke(startDate: String, endDate: String): Flow<StatsData> {
        return timerRepository.getRecordsByDateRange(startDate, endDate).map { records ->
            val slices = records
                .filter { it.duration > 0 }
                .groupBy { it.taskId }
                .map { (taskId, taskRecords) ->
                    val first = taskRecords.first()
                    TaskTimeSlice(
                        taskId = taskId,
                        taskName = first.taskName.ifEmpty { "已删除" },
                        color = first.taskColor.ifEmpty { "#CCCCCC" },
                        seconds = taskRecords.sumOf { it.duration } / 1000
                    )
                }
                .sortedByDescending { it.seconds }

            StatsData(
                totalSeconds = slices.sumOf { it.seconds },
                taskBreakdown = slices
            )
        }
    }
}
