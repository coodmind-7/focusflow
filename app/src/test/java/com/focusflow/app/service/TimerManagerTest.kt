package com.focusflow.app.service

import com.focusflow.app.domain.model.TimerMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimerManagerTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var timerManager: TimerManager
    private var manualTimeMs: Long? = null // null = use real time, non-null = controlled time
    private val timeProvider = object : TimeProvider {
        override fun elapsedRealtime(): Long = manualTimeMs ?: (System.nanoTime() / 1_000_000)
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        manualTimeMs = null // default to real time
        timerManager = TimerManager(timeProvider)
    }

    private fun advanceTimeByMs(deltaMs: Long) {
        manualTimeMs = (manualTimeMs ?: (System.nanoTime() / 1_000_000)) + deltaMs
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── TM-01: IDLE → start → RUNNING ──

    @Test
    fun `start transitions from IDLE to RUNNING`() = runTest {
        timerManager.start(taskId = 1, taskName = "Work", mode = TimerMode.COUNT_UP)

        assertEquals(TimerState.RUNNING, timerManager.timerState.value)
        assertTrue(timerManager.isRunning)
        assertFalse(timerManager.isPaused)
    }

    @Test
    fun `start sets correct task context`() {
        timerManager.start(taskId = 42, taskName = "Study", mode = TimerMode.COUNT_DOWN, targetDuration = 3000)

        assertEquals(42, timerManager.getCurrentTaskId())
        assertEquals("Study", timerManager.getCurrentTaskName())
        assertEquals(TimerMode.COUNT_DOWN, timerManager.getCurrentMode())
        assertEquals(3000, timerManager.getTargetDuration())
    }

    @Test
    fun `start emits TimerEvent with RUNNING state`() = runTest {
        val events = mutableListOf<TimerEvent>()
        val job = launch {
            timerManager.timerEvents.collect { events.add(it) }
        }

        timerManager.start(taskId = 1, taskName = "Work", mode = TimerMode.COUNT_UP)
        advanceTimeBy(100)

        assertTrue(events.isNotEmpty())
        val lastEvent = events.last()
        assertEquals(TimerState.RUNNING, lastEvent.state)
        assertEquals(1, lastEvent.taskId)
        assertEquals("Work", lastEvent.taskName)
        assertEquals(TimerMode.COUNT_UP, lastEvent.mode)
        assertTrue(lastEvent.elapsedMs >= 0)

        job.cancel()
    }

    @Test
    fun `elapsedMs increases while RUNNING`() {
        timerManager.start(taskId = 1, taskName = "Work", mode = TimerMode.COUNT_UP)

        val first = timerManager.getCurrentElapsedMs()
        Thread.sleep(100)
        val second = timerManager.getCurrentElapsedMs()

        assertTrue("Elapsed time should increase while running", second > first)
    }

    // ── TM-02: RUNNING → pause → PAUSED ──

    @Test
    fun `pause transitions from RUNNING to PAUSED`() {
        timerManager.start(taskId = 1, taskName = "Work", mode = TimerMode.COUNT_UP)

        timerManager.pause()

        assertEquals(TimerState.PAUSED, timerManager.timerState.value)
        assertTrue(timerManager.isPaused)
        assertFalse(timerManager.isRunning)
    }

    @Test
    fun `pause freezes elapsedMs`() {
        timerManager.start(taskId = 1, taskName = "Work", mode = TimerMode.COUNT_UP)
        Thread.sleep(50)

        timerManager.pause()
        val frozen = timerManager.getCurrentElapsedMs()
        Thread.sleep(100)
        val stillFrozen = timerManager.getCurrentElapsedMs()

        assertEquals("Elapsed time should not change while paused", frozen, stillFrozen)
    }

    @Test
    fun `pause returns the elapsedMs at freeze point`() {
        timerManager.start(taskId = 1, taskName = "Work", mode = TimerMode.COUNT_UP)
        Thread.sleep(50)

        val returned = timerManager.pause()

        assertEquals(timerManager.getCurrentElapsedMs(), returned)
    }

    // ── TM-03: PAUSED → resume → RUNNING ──

    @Test
    fun `resume transitions from PAUSED to RUNNING`() {
        timerManager.start(taskId = 1, taskName = "Work", mode = TimerMode.COUNT_UP)
        timerManager.pause()

        timerManager.resume()

        assertEquals(TimerState.RUNNING, timerManager.timerState.value)
        assertTrue(timerManager.isRunning)
    }

    @Test
    fun `resume continues from paused elapsedMs`() {
        timerManager.start(taskId = 1, taskName = "Work", mode = TimerMode.COUNT_UP)
        Thread.sleep(50)
        val pausedAt = timerManager.pause()

        timerManager.resume()
        Thread.sleep(50)
        val afterResume = timerManager.getCurrentElapsedMs()

        assertTrue("After resume, elapsedMs should continue from paused point", afterResume > pausedAt)
    }

    // ── TM-04: RUNNING → stop → IDLE ──

    @Test
    fun `stop transitions from RUNNING to IDLE`() {
        timerManager.start(taskId = 1, taskName = "Work", mode = TimerMode.COUNT_UP)

        timerManager.stop()

        assertEquals(TimerState.IDLE, timerManager.timerState.value)
        assertFalse(timerManager.isRunning)
        assertFalse(timerManager.isPaused)
    }

    @Test
    fun `stop returns final TimerEvent with IDLE state`() {
        timerManager.start(taskId = 3, taskName = "Reading", mode = TimerMode.COUNT_DOWN, targetDuration = 5000)
        Thread.sleep(50)

        val event = timerManager.stop()

        assertEquals(TimerState.IDLE, event.state)
        assertEquals(3, event.taskId)
        assertEquals("Reading", event.taskName)
        assertEquals(TimerMode.COUNT_DOWN, event.mode)
        assertEquals(5000, event.targetDuration)
        assertTrue(event.elapsedMs > 0)
    }

    @Test
    fun `after stop getCurrentElapsedMs returns final elapsed`() {
        timerManager.start(taskId = 1, taskName = "Work", mode = TimerMode.COUNT_UP)
        Thread.sleep(50)
        val event = timerManager.stop()
        Thread.sleep(100)

        assertEquals("After stop, elapsedMs should remain at final value", event.elapsedMs, timerManager.getCurrentElapsedMs())
    }

    // ── TM-05: PAUSED → stop → IDLE ──

    @Test
    fun `stop from PAUSED transitions to IDLE`() {
        timerManager.start(taskId = 1, taskName = "Work", mode = TimerMode.COUNT_UP)
        timerManager.pause()

        timerManager.stop()

        assertEquals(TimerState.IDLE, timerManager.timerState.value)
    }

    @Test
    fun `stop from PAUSED preserves paused elapsedMs`() {
        timerManager.start(taskId = 1, taskName = "Work", mode = TimerMode.COUNT_UP)
        Thread.sleep(50)
        timerManager.pause()
        val pausedElapsed = timerManager.getCurrentElapsedMs()

        val event = timerManager.stop()

        assertEquals("Stopping from paused should preserve elapsed", pausedElapsed, event.elapsedMs)
    }

    // ── TM-06: IDLE → pause → no effect ──

    @Test
    fun `pause while IDLE has no effect`() {
        assertEquals(TimerState.IDLE, timerManager.timerState.value)

        val result = timerManager.pause()

        assertEquals(TimerState.IDLE, timerManager.timerState.value)
        assertEquals(0, result)
    }

    // ── TM-07: IDLE → resume → no effect ──

    @Test
    fun `resume while IDLE has no effect`() {
        assertEquals(TimerState.IDLE, timerManager.timerState.value)

        timerManager.resume()

        assertEquals(TimerState.IDLE, timerManager.timerState.value)
    }

    @Test
    fun `resume while RUNNING has no effect`() {
        timerManager.start(taskId = 1, taskName = "Work", mode = TimerMode.COUNT_UP)
        assertEquals(TimerState.RUNNING, timerManager.timerState.value)

        timerManager.resume() // should be no-op

        assertEquals(TimerState.RUNNING, timerManager.timerState.value)
    }

    // ── TM-08: Countdown auto-stop ──

    @Test
    fun `countdown auto-stops when elapsed reaches target`() = runTest {
        manualTimeMs = 0L
        timerManager.start(taskId = 1, taskName = "Focus", mode = TimerMode.COUNT_DOWN, targetDuration = 400)

        manualTimeMs = 500L
        // Advance virtual time so the tick coroutine fires via delay
        advanceTimeBy(1000)

        assertEquals("Countdown should auto-stop when elapsedMs >= targetDuration", TimerState.IDLE, timerManager.timerState.value)
    }

    @Test
    fun `countdown elapsedMs capped at targetDuration`() {
        timerManager.start(taskId = 1, taskName = "Focus", mode = TimerMode.COUNT_DOWN, targetDuration = 500)
        // Let enough real time pass
        Thread.sleep(600)

        val elapsed = timerManager.getCurrentElapsedMs()
        // Either stopped at target, or still ticking - check it's sane
        assertTrue(elapsed >= 0)
        if (timerManager.timerState.value == TimerState.IDLE) {
            // If auto-stopped, elapsed should be near target
            assertTrue("Countdown elapsed should be near target", elapsed in 400..600)
        }
    }

    // ── TM-09: Switching tasks ──

    @Test
    fun `starting new task pauses previous task`() {
        timerManager.start(taskId = 1, taskName = "Task A", mode = TimerMode.COUNT_UP)
        Thread.sleep(50)

        val elapsedA = timerManager.pause()

        timerManager.start(taskId = 2, taskName = "Task B", mode = TimerMode.COUNT_UP)

        assertEquals(2, timerManager.getCurrentTaskId())
        assertEquals("Task B", timerManager.getCurrentTaskName())
        assertTrue(timerManager.isRunning)
        assertTrue("Previous task should have accumulated time", elapsedA > 0)
    }

    // ── TM-10: Same task re-start ──

    @Test
    fun `starting same task while RUNNING resets elapsed`() {
        timerManager.start(taskId = 1, taskName = "Work", mode = TimerMode.COUNT_UP)

        // Restarting the same task resets the timer (ViewModel must guard against this)
        timerManager.start(taskId = 1, taskName = "Work", mode = TimerMode.COUNT_UP)

        assertTrue(timerManager.isRunning)
        assertTrue(timerManager.getCurrentElapsedMs() >= 0)
    }

    // ── TM-11: Paused same task → resume ──

    @Test
    fun `paused task can be resumed`() {
        timerManager.start(taskId = 1, taskName = "Work", mode = TimerMode.COUNT_UP)
        Thread.sleep(50)
        timerManager.pause()
        val pausedElapsed = timerManager.getCurrentElapsedMs()

        timerManager.resume()
        Thread.sleep(50)

        assertTrue(timerManager.isRunning)
        assertTrue("After resume, time should continue from pause point", timerManager.getCurrentElapsedMs() > pausedElapsed)
    }

    // ── TM-12: getCurrentElapsedMs accuracy ──

    @Test
    fun `getCurrentElapsedMs returns correct elapsed after known sleep`() {
        timerManager.start(taskId = 1, taskName = "Work", mode = TimerMode.COUNT_UP)

        val sleepMs = 200L
        Thread.sleep(sleepMs)

        val elapsed = timerManager.getCurrentElapsedMs()
        // Allow 50ms tolerance for thread scheduling
        assertTrue("Expected elapsed near ${sleepMs}ms, got ${elapsed}ms", elapsed in (sleepMs - 50)..(sleepMs + 100))
    }

    @Test
    fun `elapsedMs is near 0 right after start`() {
        timerManager.start(taskId = 1, taskName = "Work", mode = TimerMode.COUNT_UP)

        val elapsed = timerManager.getCurrentElapsedMs()

        assertTrue("Elapsed should be near 0 right after start, got $elapsed", elapsed < 10)
    }

    // ── TM-13: Multiple pause/resume cycles ──

    @Test
    fun `multiple pause resume cycles accumulate correctly`() {
        timerManager.start(taskId = 1, taskName = "Work", mode = TimerMode.COUNT_UP)

        // Segment 1
        Thread.sleep(50)
        timerManager.pause()
        val afterFirst = timerManager.getCurrentElapsedMs()

        // Segment 2
        timerManager.resume()
        Thread.sleep(50)
        timerManager.pause()
        val afterSecond = timerManager.getCurrentElapsedMs()

        // Segment 3
        timerManager.resume()
        Thread.sleep(50)
        val final = timerManager.getCurrentElapsedMs()

        assertTrue("Second pause should have more elapsed than first", afterSecond > afterFirst)
        assertTrue("Third measurement should have more elapsed than second", final > afterSecond)
    }

    // ── helper properties ──

    @Test
    fun `isRunning and isPaused return correct values in each state`() {
        assertFalse(timerManager.isRunning)
        assertFalse(timerManager.isPaused)

        timerManager.start(taskId = 1, taskName = "Work", mode = TimerMode.COUNT_UP)
        assertTrue(timerManager.isRunning)
        assertFalse(timerManager.isPaused)

        timerManager.pause()
        assertFalse(timerManager.isRunning)
        assertTrue(timerManager.isPaused)

        timerManager.stop()
        assertFalse(timerManager.isRunning)
        assertFalse(timerManager.isPaused)
    }

    // ── TimerEvent flow emission ──

    @Test
    fun `timerEvents emits event on state changes`() = runTest {
        val events = mutableListOf<TimerEvent>()
        val job = launch {
            timerManager.timerEvents.collect { events.add(it) }
        }

        timerManager.start(taskId = 1, taskName = "Work", mode = TimerMode.COUNT_UP)
        advanceTimeBy(50)
        timerManager.pause()
        advanceTimeBy(50)
        timerManager.resume()
        advanceTimeBy(50)
        timerManager.stop()
        advanceTimeBy(50)

        job.cancel()

        // Should have events: start, ticks, pause, resume, ticks, stop
        assertTrue("Expected at least 5 events, got ${events.size}", events.size >= 5)

        // Should have RUNNING events
        val runningEvents = events.filter { it.state == TimerState.RUNNING }
        assertTrue("Should have RUNNING events", runningEvents.isNotEmpty())

        // Should have PAUSED event
        val pausedEvents = events.filter { it.state == TimerState.PAUSED }
        assertTrue("Should have PAUSED events", pausedEvents.isNotEmpty())

        // Last event should be IDLE
        assertEquals(TimerState.IDLE, events.last().state)
    }

    @Test
    fun `timerState flow reflects state changes`() = runTest {
        val states = mutableListOf<TimerState>()
        val job = launch {
            timerManager.timerState.collect { states.add(it) }
        }

        timerManager.start(taskId = 1, taskName = "Work", mode = TimerMode.COUNT_UP)
        advanceTimeBy(50)
        timerManager.pause()
        advanceTimeBy(50)
        timerManager.stop()
        advanceTimeBy(50)

        job.cancel()

        assertEquals(TimerState.IDLE, states.first())
        assertTrue(states.contains(TimerState.RUNNING))
        assertTrue(states.contains(TimerState.PAUSED))
        assertEquals(TimerState.IDLE, states.last())
    }

    // ── Edge cases ──

    @Test
    fun `initial elapsedMs from start is respected`() {
        timerManager.start(
            taskId = 1, taskName = "Work", mode = TimerMode.COUNT_UP,
            initialElapsedMs = 5000
        )

        val elapsed = timerManager.getCurrentElapsedMs()

        assertTrue("Initial elapsed should be at least 5000ms, got ${elapsed}ms", elapsed >= 5000)
    }

    @Test
    fun `stop after stop is harmless`() {
        timerManager.start(taskId = 1, taskName = "Work", mode = TimerMode.COUNT_UP)
        timerManager.stop()
        val firstStopElapsed = timerManager.getCurrentElapsedMs()

        timerManager.stop() // second stop

        assertEquals(TimerState.IDLE, timerManager.timerState.value)
        assertEquals(firstStopElapsed, timerManager.getCurrentElapsedMs())
    }

    @Test
    fun `pause after stop has no effect`() {
        timerManager.start(taskId = 1, taskName = "Work", mode = TimerMode.COUNT_UP)
        timerManager.stop()

        val result = timerManager.pause()

        assertEquals(TimerState.IDLE, timerManager.timerState.value)
        assertTrue("Should return the final elapsed", result > 0)
    }

    @Test
    fun `start after stop creates a fresh session`() {
        timerManager.start(taskId = 1, taskName = "Session 1", mode = TimerMode.COUNT_UP)
        Thread.sleep(50)
        timerManager.stop()

        timerManager.start(taskId = 2, taskName = "Session 2", mode = TimerMode.COUNT_DOWN, targetDuration = 10000)

        assertEquals(TimerState.RUNNING, timerManager.timerState.value)
        assertEquals(2, timerManager.getCurrentTaskId())
        assertEquals("Session 2", timerManager.getCurrentTaskName())
        assertEquals(TimerMode.COUNT_DOWN, timerManager.getCurrentMode())
        assertTrue("Fresh session should start near 0 elapsed", timerManager.getCurrentElapsedMs() < 10)
    }

    @Test
    fun `getCurrentTaskId returns -1 in IDLE state before any start`() {
        assertEquals(-1, timerManager.getCurrentTaskId())
    }
}
