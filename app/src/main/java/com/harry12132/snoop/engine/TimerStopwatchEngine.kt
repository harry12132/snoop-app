package com.harry12132.snoop.engine

import android.content.Context
import android.os.SystemClock
import com.harry12132.snoop.model.StopwatchState
import com.harry12132.snoop.model.TimerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.ceil

/**
 * Process-resilient timer and stopwatch state.
 *
 * A timer is persisted as an absolute deadline, so it keeps counting down
 * while Snoop is stopped and can recover after the process is recreated.
 */
class TimerStopwatchEngine(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "snoop_clock_state",
        Context.MODE_PRIVATE
    )
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Timer state
    private var timerDeadlineEpochMs = 0L
    private var timerJob: Job? = null
    private val _timerState = MutableStateFlow(loadTimerState())
    val timerState: StateFlow<TimerState> = _timerState

    // Stopwatch state
    private var stopwatchStartElapsedMs = 0L
    private var accumulatedStopwatchMs = 0L
    private var stopwatchJob: Job? = null
    private val _stopwatchState = MutableStateFlow(loadStopwatchState())
    val stopwatchState: StateFlow<StopwatchState> = _stopwatchState

    init {
        if (_timerState.value.isRunning) runTimerLoop()
        if (_stopwatchState.value.isRunning) runStopwatchLoop()
    }

    private fun timerState(
        label: String,
        total: Int,
        remaining: Int,
        running: Boolean,
        completed: Boolean
    ) = TimerState(
        label = label,
        totalSeconds = total,
        remainingSeconds = remaining,
        isRunning = running,
        isCompleted = completed,
        onStartPause = { toggleTimerPause() },
        onReset = { resetTimer() },
        onAddMinute = { addTimerSeconds(60) }
    )

    private fun loadTimerState(): TimerState {
        val total = prefs.getInt(KEY_TIMER_TOTAL, 300).coerceAtLeast(1)
        val label = prefs.getString(KEY_TIMER_LABEL, "Timer") ?: "Timer"
        val savedRunning = prefs.getBoolean(KEY_TIMER_RUNNING, false)
        val savedRemaining = prefs.getInt(KEY_TIMER_REMAINING, total).coerceAtLeast(0)
        val deadline = prefs.getLong(KEY_TIMER_DEADLINE, 0L)

        if (savedRunning && deadline > 0L) {
            val remaining = secondsUntil(deadline)
            if (remaining > 0) {
                timerDeadlineEpochMs = deadline
                return timerState(label, total, remaining, true, false)
            }
            persistTimer(total, 0, label, false, 0L)
            return timerState(label, total, 0, false, true)
        }
        return timerState(label, total, savedRemaining, false, savedRemaining == 0)
    }

    fun startTimer(seconds: Int = 300, label: String = "Timer") {
        val safeSeconds = seconds.coerceAtLeast(1)
        timerJob?.cancel()
        timerDeadlineEpochMs = System.currentTimeMillis() + safeSeconds * 1000L
        _timerState.value = timerState(label, safeSeconds, safeSeconds, true, false)
        persistCurrentTimer()
        runTimerLoop()
    }

    fun toggleTimerPause() {
        val current = _timerState.value
        if (current.isRunning) {
            val remaining = secondsUntil(timerDeadlineEpochMs)
            timerJob?.cancel()
            timerDeadlineEpochMs = 0L
            _timerState.value = timerState(
                current.label,
                current.totalSeconds,
                remaining,
                false,
                remaining == 0
            )
            persistCurrentTimer()
        } else {
            val remaining = current.remainingSeconds
            if (remaining <= 0) {
                resetTimer()
                return
            }
            timerDeadlineEpochMs = System.currentTimeMillis() + remaining * 1000L
            _timerState.value = current.copy(isRunning = true, isCompleted = false)
            persistCurrentTimer()
            runTimerLoop()
        }
    }

    private fun runTimerLoop() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive && _timerState.value.isRunning) {
                val remaining = secondsUntil(timerDeadlineEpochMs)
                val current = _timerState.value
                if (remaining != current.remainingSeconds) {
                    _timerState.value = current.copy(remainingSeconds = remaining)
                }
                if (remaining <= 0) {
                    _timerState.value = current.copy(
                        remainingSeconds = 0,
                        isRunning = false,
                        isCompleted = true
                    )
                    timerDeadlineEpochMs = 0L
                    persistCurrentTimer()
                    break
                }
                delay(200L)
            }
        }
    }

    fun resetTimer() {
        timerJob?.cancel()
        timerDeadlineEpochMs = 0L
        val current = _timerState.value
        _timerState.value = timerState(
            current.label,
            current.totalSeconds,
            current.totalSeconds,
            false,
            false
        )
        persistCurrentTimer()
    }

    fun stopTimer() {
        timerJob?.cancel()
        timerDeadlineEpochMs = 0L
        _timerState.value = timerState("Timer", 300, 300, false, false)
        persistCurrentTimer()
    }

    fun addTimerSeconds(seconds: Int) {
        if (seconds <= 0) return
        val current = _timerState.value
        val remaining = if (current.isRunning) secondsUntil(timerDeadlineEpochMs) else current.remainingSeconds
        val newRemaining = (remaining + seconds).coerceAtMost(24 * 60 * 60)
        val newTotal = (current.totalSeconds + seconds).coerceAtMost(24 * 60 * 60)
        timerDeadlineEpochMs = System.currentTimeMillis() + newRemaining * 1000L
        _timerState.value = timerState(current.label, newTotal, newRemaining, true, false)
        persistCurrentTimer()
        runTimerLoop()
    }

    private fun persistCurrentTimer() {
        val state = _timerState.value
        persistTimer(
            state.totalSeconds,
            state.remainingSeconds,
            state.label,
            state.isRunning,
            if (state.isRunning) timerDeadlineEpochMs else 0L
        )
    }

    private fun persistTimer(total: Int, remaining: Int, label: String, running: Boolean, deadline: Long) {
        prefs.edit()
            .putInt(KEY_TIMER_TOTAL, total)
            .putInt(KEY_TIMER_REMAINING, remaining)
            .putString(KEY_TIMER_LABEL, label)
            .putBoolean(KEY_TIMER_RUNNING, running)
            .putLong(KEY_TIMER_DEADLINE, deadline)
            .apply()
    }

    private fun secondsUntil(deadlineEpochMs: Long): Int {
        if (deadlineEpochMs <= 0L) return 0
        return ceil((deadlineEpochMs - System.currentTimeMillis()).coerceAtLeast(0L) / 1000.0)
            .toInt()
    }

    // Stopwatch
    private fun stopwatchState(
        elapsed: Long,
        running: Boolean,
        laps: List<Long>
    ) = StopwatchState(
        elapsedMillis = elapsed,
        isRunning = running,
        laps = laps,
        onStartPause = { toggleStopwatchPause() },
        onReset = { resetStopwatch() },
        onLap = { recordLap() }
    )

    private fun loadStopwatchState(): StopwatchState {
        val savedElapsed = prefs.getLong(KEY_STOPWATCH_ELAPSED, 0L).coerceAtLeast(0L)
        val savedRunning = prefs.getBoolean(KEY_STOPWATCH_RUNNING, false)
        val savedStartEpoch = prefs.getLong(KEY_STOPWATCH_START_EPOCH, 0L)
        val laps = prefs.getString(KEY_STOPWATCH_LAPS, "")
            ?.split(",")
            ?.mapNotNull { it.toLongOrNull() }
            ?: emptyList()

        accumulatedStopwatchMs = if (savedRunning && savedStartEpoch > 0L) {
            savedElapsed + (System.currentTimeMillis() - savedStartEpoch).coerceAtLeast(0L)
        } else {
            savedElapsed
        }

        if (savedRunning) {
            stopwatchStartElapsedMs = SystemClock.elapsedRealtime()
            persistStopwatch(true)
        }

        return stopwatchState(accumulatedStopwatchMs, savedRunning, laps)
    }

    fun startStopwatch() {
        if (_stopwatchState.value.isRunning) return
        accumulatedStopwatchMs = _stopwatchState.value.elapsedMillis
        stopwatchStartElapsedMs = SystemClock.elapsedRealtime()
        _stopwatchState.value = stopwatchState(
            accumulatedStopwatchMs,
            true,
            _stopwatchState.value.laps
        )
        persistStopwatch(true)
        runStopwatchLoop()
    }

    fun toggleStopwatchPause() {
        val current = _stopwatchState.value
        if (current.isRunning) {
            accumulatedStopwatchMs += (SystemClock.elapsedRealtime() - stopwatchStartElapsedMs).coerceAtLeast(0L)
            stopwatchJob?.cancel()
            _stopwatchState.value = stopwatchState(accumulatedStopwatchMs, false, current.laps)
            persistStopwatch(false)
        } else {
            startStopwatch()
        }
    }

    private fun runStopwatchLoop() {
        stopwatchJob?.cancel()
        stopwatchJob = scope.launch {
            while (isActive && _stopwatchState.value.isRunning) {
                val elapsed = accumulatedStopwatchMs +
                    (SystemClock.elapsedRealtime() - stopwatchStartElapsedMs).coerceAtLeast(0L)
                _stopwatchState.value = _stopwatchState.value.copy(elapsedMillis = elapsed)
                delay(100L)
            }
        }
    }

    fun recordLap() {
        val current = _stopwatchState.value
        if (current.elapsedMillis <= 0L) return
        _stopwatchState.value = current.copy(laps = current.laps + current.elapsedMillis)
        persistStopwatch(current.isRunning)
    }

    fun resetStopwatch() {
        stopwatchJob?.cancel()
        accumulatedStopwatchMs = 0L
        stopwatchStartElapsedMs = 0L
        _stopwatchState.value = stopwatchState(0L, false, emptyList())
        persistStopwatch(false)
    }

    private fun persistStopwatch(running: Boolean) {
        val state = _stopwatchState.value
        val elapsed = if (running) accumulatedStopwatchMs else state.elapsedMillis
        prefs.edit()
            .putLong(KEY_STOPWATCH_ELAPSED, elapsed)
            .putBoolean(KEY_STOPWATCH_RUNNING, running)
            .putLong(KEY_STOPWATCH_START_EPOCH, if (running) System.currentTimeMillis() else 0L)
            .putString(KEY_STOPWATCH_LAPS, state.laps.joinToString(","))
            .apply()
    }

    fun dispose() {
        timerJob?.cancel()
        stopwatchJob?.cancel()
        scope.cancel()
    }

    private companion object {
        const val KEY_TIMER_TOTAL = "timer_total"
        const val KEY_TIMER_REMAINING = "timer_remaining"
        const val KEY_TIMER_LABEL = "timer_label"
        const val KEY_TIMER_RUNNING = "timer_running"
        const val KEY_TIMER_DEADLINE = "timer_deadline"
        const val KEY_STOPWATCH_ELAPSED = "stopwatch_elapsed"
        const val KEY_STOPWATCH_RUNNING = "stopwatch_running"
        const val KEY_STOPWATCH_START_EPOCH = "stopwatch_start_epoch"
        const val KEY_STOPWATCH_LAPS = "stopwatch_laps"
    }
}
