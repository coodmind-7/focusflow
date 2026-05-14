package com.focusflow.app.ui.timer

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.viewModelScope
import com.focusflow.app.data.local.preferences.UserPreferences
import com.focusflow.app.domain.model.TaskLabel
import com.focusflow.app.domain.model.TimerMode
import com.focusflow.app.domain.model.TimerRecord
import com.focusflow.app.domain.usecase.task.CreateTaskUseCase
import com.focusflow.app.domain.usecase.task.DeleteTaskUseCase
import com.focusflow.app.domain.usecase.task.GetTasksUseCase
import com.focusflow.app.domain.usecase.task.ReorderTasksUseCase
import com.focusflow.app.domain.usecase.task.UpdateTaskUseCase
import com.focusflow.app.domain.usecase.timer.GetTodayRecordsUseCase
import com.focusflow.app.domain.usecase.timer.RecordDailyGoalUseCase
import com.focusflow.app.domain.usecase.timer.StartTimerUseCase
import com.focusflow.app.domain.usecase.timer.StopTimerUseCase
import com.focusflow.app.service.TimerEvent
import com.focusflow.app.service.TimerForegroundService
import com.focusflow.app.service.TimerManager
import com.focusflow.app.service.TimerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class TimerUiState(
    val tasks: List<TaskLabel> = emptyList(),
    val timerState: TimerState = TimerState.IDLE,
    val elapsedMs: Long = 0L,
    val mode: TimerMode = TimerMode.COUNT_UP,
    val targetDuration: Long = 0L,
    val runningTaskId: Long = -1L,
    val runningTaskName: String = "",
    val todayTotalSeconds: Long = 0L,
    val dailyGoalSeconds: Long = UserPreferences.DEFAULT_GOAL_SECONDS,
    val longPressedTaskId: Long = -1L,
    val showEditDialog: Boolean = false,
    val editingTask: TaskLabel? = null,
    val showGoalDialog: Boolean = false,
    val showGoalCelebration: Boolean = false
)

private data class TaskTimerSession(
    val recordId: Long,
    val elapsedMs: Long
)

@HiltViewModel
class TimerViewModel @Inject constructor(
    application: Application,
    private val getTasksUseCase: GetTasksUseCase,
    private val createTaskUseCase: CreateTaskUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val reorderTasksUseCase: ReorderTasksUseCase,
    private val startTimerUseCase: StartTimerUseCase,
    private val stopTimerUseCase: StopTimerUseCase,
    private val getTodayRecordsUseCase: GetTodayRecordsUseCase,
    private val recordDailyGoalUseCase: RecordDailyGoalUseCase,
    private val timerManager: TimerManager,
    private val userPreferences: UserPreferences
) : AndroidViewModel(application) {

    private val _longPressedTaskId = MutableStateFlow(-1L)
    private val _showEditDialog = MutableStateFlow(false)
    private val _editingTask = MutableStateFlow<TaskLabel?>(null)
    private val _showGoalDialog = MutableStateFlow(false)
    private val _showGoalCelebration = MutableStateFlow(false)
    private var runningRecordId: Long = -1
    private var currentRecordSavedDuration: Long = 0L

    private val taskSessions = mutableMapOf<Long, TaskTimerSession>()
    private val _pausedTaskIds = MutableStateFlow<Set<Long>>(emptySet())
    val pausedTaskIds: StateFlow<Set<Long>> = _pausedTaskIds.asStateFlow()

    private var wasPausedByLifecycle = false

    private val appLifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) {
            if (timerManager.isRunning && runningRecordId != -1L) {
                val elapsed = timerManager.pause()
                currentRecordSavedDuration = elapsed
                viewModelScope.launch {
                    stopTimerUseCase.updateDuration(runningRecordId, elapsed)
                }
                val taskId = timerManager.getCurrentTaskId()
                taskSessions[taskId] = TaskTimerSession(runningRecordId, elapsed)
                runningRecordId = -1
                _pausedTaskIds.value = taskSessions.keys.toSet()
                wasPausedByLifecycle = true
            }
        }

        override fun onStart(owner: LifecycleOwner) {
            if (wasPausedByLifecycle && timerManager.isPaused) {
                val taskId = timerManager.getCurrentTaskId()
                val task = uiState.value.tasks.find { it.id == taskId }
                if (task != null) {
                    viewModelScope.launch {
                        val session = taskSessions.remove(taskId)
                        if (session != null) {
                            runningRecordId = session.recordId
                            currentRecordSavedDuration = session.elapsedMs
                        }
                        _pausedTaskIds.value = taskSessions.keys.toSet()
                        timerManager.resume()
                        startForegroundService()
                    }
                }
                wasPausedByLifecycle = false
            }
        }
    }

    private val todayDate: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    private val tasks: StateFlow<List<TaskLabel>> = getTasksUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val todayTotalSeconds = combine(
        getTodayRecordsUseCase(todayDate).map { records -> records.sumOf { it.duration } },
        timerManager.timerEvents
    ) { dbSumMs, event ->
        val liveElapsed = if (event.state == TimerState.RUNNING || event.state == TimerState.PAUSED) event.elapsedMs else 0L
        (dbSumMs + liveElapsed - currentRecordSavedDuration) / 1000
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    private val dailyGoalSeconds = userPreferences.dailyGoalSeconds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences.DEFAULT_GOAL_SECONDS)

    val uiState: StateFlow<TimerUiState> = combine(
        combine(
            combine(tasks, _longPressedTaskId, _showEditDialog) { t, l, s -> Triple(t, l, s) },
            combine(_editingTask, _showGoalDialog, _showGoalCelebration) { e, g, c -> Triple(e, g, c) }
        ) { (t, l, s), (e, g, c) ->
            Hex(t, l, s, e, g, c)
        },
        combine(timerManager.timerState, timerManager.timerEvents, todayTotalSeconds, dailyGoalSeconds) { ts, te, tt, dg ->
            Quad(ts, te, tt, dg)
        }
    ) { (t, l, s, e, g, c), (ts, te, tt, dg) ->
        TimerUiState(
            tasks = t,
            timerState = ts,
            elapsedMs = te.elapsedMs,
            mode = te.mode,
            targetDuration = te.targetDuration,
            runningTaskId = te.taskId,
            runningTaskName = te.taskName,
            todayTotalSeconds = tt,
            dailyGoalSeconds = dg,
            longPressedTaskId = l,
            showEditDialog = s,
            editingTask = e,
            showGoalDialog = g,
            showGoalCelebration = c
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TimerUiState())

    init {
        // Periodically persist running elapsed so stats stay current
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(3_600_000)
                if (timerManager.isRunning && runningRecordId != -1L) {
                    val elapsed = timerManager.getCurrentElapsedMs()
                    stopTimerUseCase.updateDuration(runningRecordId, elapsed)
                    currentRecordSavedDuration = elapsed
                }
            }
        }
        viewModelScope.launch {
            timerManager.timerEvents.collect { event ->
                if (event.state == TimerState.IDLE && runningRecordId != -1L) {
                    val recordId = runningRecordId
                    runningRecordId = -1
                    currentRecordSavedDuration = 0
                    stopTimerUseCase(recordId, System.currentTimeMillis(), event.elapsedMs)
                    taskSessions.remove(event.taskId)
                    _pausedTaskIds.value = taskSessions.keys.toSet()
                    checkGoalAchievement()
                }
            }
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)
    }

    // ── Timer actions ──

    fun startTimer(task: TaskLabel) {
        viewModelScope.launch {
            // If same task is paused, just resume
            if (task.id == timerManager.getCurrentTaskId() && timerManager.isPaused) {
                val session = taskSessions.remove(task.id)
                if (session != null) {
                    runningRecordId = session.recordId
                    currentRecordSavedDuration = session.elapsedMs
                }
                _pausedTaskIds.value = taskSessions.keys.toSet()
                timerManager.resume()
                startForegroundService()
                return@launch
            }

            // If same task is already running, do nothing
            if (task.id == timerManager.getCurrentTaskId() && timerManager.isRunning) return@launch

            // Pause current running task and save progress
            if (timerManager.isRunning || timerManager.isPaused) {
                val currentId = timerManager.getCurrentTaskId()
                val elapsed = if (timerManager.isRunning) {
                    timerManager.pause()
                } else {
                    timerManager.getCurrentElapsedMs()
                }
                if (runningRecordId != -1L) {
                    stopTimerUseCase.updateDuration(runningRecordId, elapsed)
                    taskSessions[currentId] = TaskTimerSession(runningRecordId, elapsed)
                    runningRecordId = -1
                }
            }

            val mode = task.defaultTimerMode
            val target = (task.defaultDurationMinutes ?: 0) * 60 * 1000L
            val session = taskSessions.remove(task.id)
            _pausedTaskIds.value = taskSessions.keys.toSet()

            if (session != null) {
                runningRecordId = session.recordId
                currentRecordSavedDuration = session.elapsedMs
                timerManager.start(task.id, task.name, mode, target, initialElapsedMs = session.elapsedMs)
            } else {
                val record = TimerRecord(
                    taskId = task.id,
                    date = todayDate,
                    startTime = System.currentTimeMillis(),
                    mode = mode,
                    targetDuration = if (mode == TimerMode.COUNT_DOWN) target else 0,
                    taskName = task.name,
                    taskColor = task.color
                )
                runningRecordId = startTimerUseCase(record)
                currentRecordSavedDuration = 0
                timerManager.start(task.id, task.name, mode, target)
            }

            startForegroundService()
        }
    }

    fun pauseTimer() {
        if (!timerManager.isRunning) return
        viewModelScope.launch {
            val elapsed = timerManager.pause()
            currentRecordSavedDuration = elapsed
            if (runningRecordId != -1L) {
                stopTimerUseCase.updateDuration(runningRecordId, elapsed)
                val taskId = timerManager.getCurrentTaskId()
                taskSessions[taskId] = TaskTimerSession(runningRecordId, elapsed)
                runningRecordId = -1
                _pausedTaskIds.value = taskSessions.keys.toSet()
            }
        }
    }

    fun resetTask(task: TaskLabel) {
        viewModelScope.launch {
            // If this task is currently active, stop it first
            if (task.id == timerManager.getCurrentTaskId() &&
                (timerManager.isRunning || timerManager.isPaused)
            ) {
                val elapsed = timerManager.getCurrentElapsedMs()
                val recordId = runningRecordId
                runningRecordId = -1
                currentRecordSavedDuration = 0
                timerManager.stop()
                if (recordId != -1L) {
                    stopTimerUseCase(recordId, System.currentTimeMillis(), elapsed)
                }
            }

            // Finalize any paused session
            val session = taskSessions.remove(task.id)
            if (session != null) {
                stopTimerUseCase(session.recordId, System.currentTimeMillis(), session.elapsedMs)
            }

            _pausedTaskIds.value = taskSessions.keys.toSet()
            _longPressedTaskId.value = -1
        }
    }

    // ── Task CRUD actions ──

    fun showCreateDialog() {
        _editingTask.value = null
        _showEditDialog.value = true
    }

    fun showEditDialog(task: TaskLabel) {
        _editingTask.value = task
        _showEditDialog.value = true
    }

    fun dismissDialog() {
        _showEditDialog.value = false
        _editingTask.value = null
    }

    fun saveTask(name: String, color: String, timerMode: TimerMode, durationMinutes: Int?) {
        viewModelScope.launch {
            val editing = _editingTask.value
            if (editing != null) {
                updateTaskUseCase(editing.copy(
                    name = name,
                    color = color,
                    defaultTimerMode = timerMode,
                    defaultDurationMinutes = durationMinutes
                ))
            } else {
                createTaskUseCase(TaskLabel(
                    name = name,
                    color = color,
                    defaultTimerMode = timerMode,
                    defaultDurationMinutes = durationMinutes
                ))
            }
            dismissDialog()
        }
    }

    fun deleteTask(task: TaskLabel) {
        viewModelScope.launch {
            // Stop timer if this task is active
            if (task.id == timerManager.getCurrentTaskId() &&
                (timerManager.isRunning || timerManager.isPaused)
            ) {
                val elapsed = timerManager.getCurrentElapsedMs()
                val recordId = runningRecordId
                runningRecordId = -1
                currentRecordSavedDuration = 0
                timerManager.stop()
                if (recordId != -1L) {
                    stopTimerUseCase(recordId, System.currentTimeMillis(), elapsed)
                }
            }
            // Clean up any paused session
            taskSessions.remove(task.id)?.let { session ->
                stopTimerUseCase(session.recordId, System.currentTimeMillis(), session.elapsedMs)
            }
            _pausedTaskIds.value = taskSessions.keys.toSet()
            deleteTaskUseCase(task)
        }
        _longPressedTaskId.value = -1
    }

    fun moveTaskUp(task: TaskLabel) {
        val list = uiState.value.tasks
        val index = list.indexOfFirst { it.id == task.id }
        if (index > 0) {
            swapAndSave(list, index, index - 1)
        }
    }

    fun moveTaskDown(task: TaskLabel) {
        val list = uiState.value.tasks
        val index = list.indexOfFirst { it.id == task.id }
        if (index < list.lastIndex) {
            swapAndSave(list, index, index + 1)
        }
    }

    private fun swapAndSave(list: List<TaskLabel>, from: Int, to: Int) {
        val mutated = list.toMutableList()
        mutated[from] = list[to].also { mutated[to] = list[from] }
        viewModelScope.launch {
            reorderTasksUseCase(mutated)
        }
    }

    // ── Long press ──

    fun toggleLongPress(taskId: Long) {
        if (taskId == timerManager.getCurrentTaskId() && timerManager.isRunning) return
        _longPressedTaskId.value = if (_longPressedTaskId.value == taskId) -1 else taskId
    }

    fun dismissLongPress() {
        _longPressedTaskId.value = -1
    }

    // ── Goal actions ──

    fun showGoalDialog() {
        _showGoalDialog.value = true
    }

    fun dismissGoalDialog() {
        _showGoalDialog.value = false
    }

    fun saveGoal(goalSeconds: Long) {
        viewModelScope.launch {
            userPreferences.setDailyGoalSeconds(goalSeconds)
            _showGoalDialog.value = false
            checkGoalAchievement()
        }
    }

    fun dismissCelebration() {
        _showGoalCelebration.value = false
    }

    private suspend fun checkGoalAchievement() {
        val total = todayTotalSeconds.value
        val goal = dailyGoalSeconds.value
        val isNewAchievement = recordDailyGoalUseCase(todayDate, goal, total)
        if (isNewAchievement) {
            _showGoalCelebration.value = true
        }
    }

    // ── Helpers ──

    private fun startForegroundService() {
        val intent = Intent(getApplication(), TimerForegroundService::class.java)
        getApplication<Application>().startForegroundService(intent)
    }

    override fun onCleared() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(appLifecycleObserver)
        super.onCleared()
    }
}

private data class Hex<A, B, C, D, E, F>(
    val first: A, val second: B, val third: C, val fourth: D, val fifth: E, val sixth: F
)
private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
