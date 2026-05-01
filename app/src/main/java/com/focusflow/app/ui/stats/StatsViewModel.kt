package com.focusflow.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusflow.app.data.local.entity.DailyGoalEntity
import com.focusflow.app.domain.model.StatsData
import com.focusflow.app.domain.model.TaskTimeSlice
import com.focusflow.app.domain.repository.TimerRepository
import com.focusflow.app.domain.usecase.timer.GetDailyGoalsUseCase
import com.focusflow.app.domain.usecase.timer.GetStatsUseCase
import com.focusflow.app.service.TimerEvent
import com.focusflow.app.service.TimerManager
import com.focusflow.app.service.TimerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject

enum class StatsPeriod { DAY, WEEK, MONTH }

data class StatsUiState(
    val period: StatsPeriod = StatsPeriod.DAY,
    val displayDate: String = "",
    val showPrev: Boolean = true,
    val showNext: Boolean = false,
    val stats: StatsData = StatsData.EMPTY,
    val goalRecords: List<DailyGoalEntity> = emptyList(),
    val achievedDaysInRange: Int = 0,
    val totalDaysInRange: Int = 0
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val getStatsUseCase: GetStatsUseCase,
    private val getDailyGoalsUseCase: GetDailyGoalsUseCase,
    private val timerManager: TimerManager,
    private val timerRepository: TimerRepository
) : ViewModel() {

    // 记录 DB 中运行记录的 duration，用于 mergeLiveElapsed 计算 delta
    private var runningRecordSavedMs: Long = 0L

    fun onEnter() {
        viewModelScope.launch {
            val record = timerRepository.getRunningRecord()
            val currentElapsed = timerManager.getCurrentElapsedMs()

            // 1. 先记录旧值，用于计算 delta
            runningRecordSavedMs = record?.duration ?: 0L

            // 2. 写库持久化
            if (timerManager.isRunning && record != null) {
                timerRepository.updateRecord(record.copy(duration = currentElapsed))
            }
            
            // 3. 写库完成后把 savedMs 同步为最新值
            //    等 Room Flow 再次发射时 delta = 0，结果与上次相同，被 distinctUntilChanged 拦截
            runningRecordSavedMs = currentElapsed
        }
    }

    private val today = LocalDate.now()
    private val _period = MutableStateFlow(StatsPeriod.DAY)
    private val _referenceDate = MutableStateFlow(today)

    private val dateFormatter = DateTimeFormatter.ofPattern("M月d日", Locale.CHINESE)
    private val dayOfWeekFormatter = DateTimeFormatter.ofPattern("EEE", Locale.CHINESE)
    private val monthFormatter = DateTimeFormatter.ofPattern("yyyy年M月", Locale.CHINESE)
    private val isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    val uiState: StateFlow<StatsUiState> = combine(_period, _referenceDate) { period, refDate ->
        computePeriod(period, refDate)
    }.flatMapLatest { info ->
        combine(
            getStatsUseCase(info.start.format(isoFormatter), info.end.format(isoFormatter)),
            getDailyGoalsUseCase.getByDateRange(info.start.format(isoFormatter), info.end.format(isoFormatter)),
            timerManager.timerEvents
        ) { stats, goals, event ->
            val enhancedStats = mergeLiveElapsed(stats, event, info)
            val achievedDays = goals.count { it.achieved }
            val totalDays = ChronoUnit.DAYS.between(info.start, info.end).toInt() + 1
            StatsUiState(
                period = _period.value,
                displayDate = info.display,
                showPrev = true,
                showNext = info.end.isBefore(today),
                stats = enhancedStats,
                goalRecords = goals,
                achievedDaysInRange = achievedDays,
                totalDaysInRange = totalDays
            )
        }
    }.distinctUntilChanged()
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState())

    private fun mergeLiveElapsed(
        stats: StatsData,
        event: TimerEvent,
        info: PeriodInfo
    ): StatsData {
        if (event.state != TimerState.RUNNING && event.state != TimerState.PAUSED) return stats

        val todayStr = today.format(isoFormatter)
        val rangeStart = info.start.format(isoFormatter)
        val rangeEnd = info.end.format(isoFormatter)
        if (todayStr < rangeStart || todayStr > rangeEnd) return stats

        val liveSeconds = event.elapsedMs / 1000
        val savedSeconds = runningRecordSavedMs / 1000
        val deltaSeconds = liveSeconds - savedSeconds
        if (deltaSeconds <= 0) return stats

        val existingSlice = stats.taskBreakdown.find { it.taskId == event.taskId }
        val newBreakdown = if (existingSlice != null) {
            stats.taskBreakdown.map { slice ->
                if (slice.taskId == event.taskId) slice.copy(seconds = slice.seconds + deltaSeconds)
                else slice
            }
        } else {
            stats.taskBreakdown + TaskTimeSlice(
                taskId = event.taskId,
                taskName = event.taskName.ifEmpty { "计时中" },
                color = "#4CAF50",
                seconds = liveSeconds
            )
        }
        return StatsData(
            totalSeconds = stats.totalSeconds + deltaSeconds,
            taskBreakdown = newBreakdown.sortedByDescending { it.seconds }
        )
    }

    fun selectPeriod(period: StatsPeriod) {
        _period.value = period
    }

    fun navigatePrev() {
        _referenceDate.value = when (_period.value) {
            StatsPeriod.DAY -> _referenceDate.value.minusDays(1)
            StatsPeriod.WEEK -> _referenceDate.value.minusWeeks(1)
            StatsPeriod.MONTH -> _referenceDate.value.minusMonths(1)
        }
    }

    fun navigateNext() {
        _referenceDate.value = when (_period.value) {
            StatsPeriod.DAY -> _referenceDate.value.plusDays(1)
            StatsPeriod.WEEK -> _referenceDate.value.plusWeeks(1)
            StatsPeriod.MONTH -> _referenceDate.value.plusMonths(1)
        }
    }

    private data class PeriodInfo(
        val start: LocalDate,
        val end: LocalDate,
        val display: String
    )

    private fun computePeriod(period: StatsPeriod, ref: LocalDate): PeriodInfo {
        return when (period) {
            StatsPeriod.DAY -> PeriodInfo(
                start = ref,
                end = ref,
                display = "${ref.format(dateFormatter)} ${ref.format(dayOfWeekFormatter)}"
            )
            StatsPeriod.WEEK -> {
                val monday = ref.with(DayOfWeek.MONDAY)
                val sunday = monday.plusDays(6)
                PeriodInfo(
                    start = monday,
                    end = sunday,
                    display = "${monday.format(dateFormatter)} - ${sunday.format(dateFormatter)}"
                )
            }
            StatsPeriod.MONTH -> {
                val first = ref.withDayOfMonth(1)
                val last = ref.withDayOfMonth(ref.lengthOfMonth())
                PeriodInfo(
                    start = first,
                    end = last,
                    display = ref.format(monthFormatter)
                )
            }
        }
    }
}
