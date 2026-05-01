package com.focusflow.app.ui.stats

import com.focusflow.app.data.local.entity.DailyGoalEntity
import com.focusflow.app.domain.model.StatsData
import com.focusflow.app.domain.model.TaskTimeSlice
import com.focusflow.app.domain.usecase.timer.GetDailyGoalsUseCase
import com.focusflow.app.domain.usecase.timer.GetStatsUseCase
import com.focusflow.app.domain.usecase.timer.SyncRunningRecordUseCase
import com.focusflow.app.service.TimerManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {

    private val getStatsUseCase: GetStatsUseCase = mockk()
    private val getDailyGoalsUseCase: GetDailyGoalsUseCase = mockk()
    private val timerManager: TimerManager = mockk(relaxed = true)
    private val syncRunningRecordUseCase: SyncRunningRecordUseCase = mockk(relaxed = true)

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun setupMocks() {
        coEvery { getStatsUseCase.invoke(any(), any()) } returns flowOf(StatsData.EMPTY)
        coEvery { getDailyGoalsUseCase.getByDateRange(any(), any()) } returns flowOf(emptyList())
    }

    // ── Period computation: DAY ──

    @Test
    fun `DAY period shows single date`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        setupMocks()
        val viewModel = StatsViewModel(getStatsUseCase, getDailyGoalsUseCase, timerManager, syncRunningRecordUseCase)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(StatsPeriod.DAY, state.period)
        assertTrue(state.displayDate.isNotEmpty())
    }

    @Test
    fun `navigatePrev goes to previous day`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        setupMocks()
        val viewModel = StatsViewModel(getStatsUseCase, getDailyGoalsUseCase, timerManager, syncRunningRecordUseCase)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val today = LocalDate.now()
        viewModel.navigatePrev()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val expectedDisplay = today.minusDays(1).format(DateTimeFormatter.ofPattern("M月d日"))
        assertTrue("Expected display to contain '$expectedDisplay', got '${state.displayDate}'",
            state.displayDate.contains(expectedDisplay))
    }

    @Test
    fun `navigateNext is disabled for today`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        setupMocks()
        val viewModel = StatsViewModel(getStatsUseCase, getDailyGoalsUseCase, timerManager, syncRunningRecordUseCase)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse("Cannot navigate beyond today", state.showNext)
    }

    // ── Period computation: WEEK ──

    @Test
    fun `WEEK period shows Monday to Sunday range`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        setupMocks()
        val viewModel = StatsViewModel(getStatsUseCase, getDailyGoalsUseCase, timerManager, syncRunningRecordUseCase)
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.selectPeriod(StatsPeriod.WEEK)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(StatsPeriod.WEEK, state.period)
        assertTrue("Week display should show a range, got: ${state.displayDate}", state.displayDate.contains("-"))
    }

    // ── Period computation: MONTH ──

    @Test
    fun `MONTH period shows year-month format`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        setupMocks()
        val viewModel = StatsViewModel(getStatsUseCase, getDailyGoalsUseCase, timerManager, syncRunningRecordUseCase)
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.selectPeriod(StatsPeriod.MONTH)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(StatsPeriod.MONTH, state.period)
        val yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年M月"))
        assertEquals(yearMonth, state.displayDate)
    }

    // ── Goal achievement counting ──

    @Test
    fun `achievedDaysInRange counts correctly`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val statsData = StatsData(100, listOf(
            TaskTimeSlice(taskId = 1, taskName = "Work", color = "#FF0000", seconds = 100)
        ))
        val goals = listOf(
            DailyGoalEntity("2024-01-01", 3600, 4000, achieved = true),
            DailyGoalEntity("2024-01-02", 3600, 2000, achieved = false),
            DailyGoalEntity("2024-01-03", 3600, 5000, achieved = true),
            DailyGoalEntity("2024-01-04", 3600, 0, achieved = false)
        )

        coEvery { getStatsUseCase.invoke(any(), any()) } returns flowOf(statsData)
        coEvery { getDailyGoalsUseCase.getByDateRange(any(), any()) } returns flowOf(goals)

        val viewModel = StatsViewModel(getStatsUseCase, getDailyGoalsUseCase, timerManager, syncRunningRecordUseCase)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.achievedDaysInRange)
        assertEquals(1, state.totalDaysInRange)  // DAY period spans 1 day
    }

    @Test
    fun `empty goals returns zero achieved`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        setupMocks()
        val viewModel = StatsViewModel(getStatsUseCase, getDailyGoalsUseCase, timerManager, syncRunningRecordUseCase)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(0, state.achievedDaysInRange)
    }

    // ── Period switch recalculates ──

    @Test
    fun `switching period recalculates with current reference date`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        setupMocks()
        val viewModel = StatsViewModel(getStatsUseCase, getDailyGoalsUseCase, timerManager, syncRunningRecordUseCase)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.selectPeriod(StatsPeriod.MONTH)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(StatsPeriod.MONTH, state.period)
    }

    // ── onEnter skips when timer not running ──

    @Test
    fun `onEnter does nothing when timer is not running`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        every { timerManager.isRunning } returns false
        val viewModel = StatsViewModel(getStatsUseCase, getDailyGoalsUseCase, timerManager, syncRunningRecordUseCase)
        viewModel.onEnter()
        // Should not crash
    }
}
