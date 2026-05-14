package com.focusflow.app.service

import android.os.SystemClock
import com.focusflow.app.domain.model.TimerMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

enum class TimerState { IDLE, RUNNING, PAUSED }

data class TimerEvent(
    val state: TimerState,
    val elapsedMs: Long,
    val taskId: Long,
    val taskName: String,
    val mode: TimerMode,
    val targetDuration: Long = 0
)

@Singleton
class TimerManager @Inject constructor() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _timerEvents = MutableStateFlow(TimerEvent(
        state = TimerState.IDLE,
        elapsedMs = 0,
        taskId = -1,
        taskName = "",
        mode = TimerMode.COUNT_UP,
        targetDuration = 0
    ))
    val timerEvents: StateFlow<TimerEvent> = _timerEvents.asStateFlow()

    private val _timerState = MutableStateFlow(TimerState.IDLE)
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private var tickJob: Job? = null

    private var baseElapsedRealtime: Long = 0
    private var savedElapsedMs: Long = 0
    private var currentTaskId: Long = -1
    private var currentTaskName: String = ""
    private var currentMode: TimerMode = TimerMode.COUNT_UP
    private var targetDuration: Long = 0

    val isRunning: Boolean get() = _timerState.value == TimerState.RUNNING
    val isPaused: Boolean get() = _timerState.value == TimerState.PAUSED

    fun start(
        taskId: Long,
        taskName: String,
        mode: TimerMode,
        targetDuration: Long = 0,
        initialElapsedMs: Long = 0
    ) {
        currentTaskId = taskId
        currentTaskName = taskName
        currentMode = mode
        this.targetDuration = targetDuration
        savedElapsedMs = initialElapsedMs
        baseElapsedRealtime = SystemClock.elapsedRealtime()
        _timerState.value = TimerState.RUNNING
        emitEvent()
        startTicking()
    }

    fun pause(): Long {
        if (_timerState.value != TimerState.RUNNING) return savedElapsedMs
        savedElapsedMs += SystemClock.elapsedRealtime() - baseElapsedRealtime
        _timerState.value = TimerState.PAUSED
        stopTicking()
        emitEvent()
        return savedElapsedMs
    }

    fun resume() {
        if (_timerState.value != TimerState.PAUSED) return
        baseElapsedRealtime = SystemClock.elapsedRealtime()
        _timerState.value = TimerState.RUNNING
        emitEvent()
        startTicking()
    }

    fun stop(): TimerEvent {
        val finalElapsed = getCurrentElapsedMs()
        savedElapsedMs = finalElapsed
        stopTicking()
        _timerState.value = TimerState.IDLE
        val event = TimerEvent(
            state = TimerState.IDLE,
            elapsedMs = finalElapsed,
            taskId = currentTaskId,
            taskName = currentTaskName,
            mode = currentMode,
            targetDuration = targetDuration
        )
        _timerEvents.tryEmit(event)
        return event
    }

    fun getCurrentElapsedMs(): Long {
        return if (_timerState.value == TimerState.RUNNING) {
            savedElapsedMs + (SystemClock.elapsedRealtime() - baseElapsedRealtime)
        } else {
            savedElapsedMs
        }
    }

    fun correctElapsedForBackground(elapsedMs: Long) {
        savedElapsedMs = elapsedMs
        baseElapsedRealtime = SystemClock.elapsedRealtime()
    }

    fun getCurrentTaskId(): Long = currentTaskId
    fun getCurrentTaskName(): String = currentTaskName
    fun getCurrentMode(): TimerMode = currentMode
    fun getTargetDuration(): Long = targetDuration

    private fun startTicking() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (true) {
                delay(200)
                val elapsed = getCurrentElapsedMs()
                if (currentMode == TimerMode.COUNT_DOWN && targetDuration > 0 && elapsed >= targetDuration) {
                    _timerEvents.tryEmit(
                        TimerEvent(
                            state = TimerState.RUNNING,
                            elapsedMs = targetDuration,
                            taskId = currentTaskId,
                            taskName = currentTaskName,
                            mode = currentMode,
                            targetDuration = targetDuration
                        )
                    )
                    stop()
                    return@launch
                }
                emitEvent()
            }
        }
    }

    private fun stopTicking() {
        tickJob?.cancel()
        tickJob = null
    }

    private fun emitEvent() {
        _timerEvents.tryEmit(
            TimerEvent(
                state = _timerState.value,
                elapsedMs = getCurrentElapsedMs(),
                taskId = currentTaskId,
                taskName = currentTaskName,
                mode = currentMode,
                targetDuration = targetDuration
            )
        )
    }
}
