package com.harry12132.snoop.engine

import com.harry12132.snoop.model.StopwatchState
import com.harry12132.snoop.model.TimerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TimerStopwatchEngine {
    private val scope = CoroutineScope(Dispatchers.Default)

    // --- TIMER ---
    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState
    private var timerJob: Job? = null

    fun startTimer(seconds: Int = 300, label: String = "Timer") {
        timerJob?.cancel()
        _timerState.value = TimerState(
            label = label,
            totalSeconds = seconds,
            remainingSeconds = seconds,
            isRunning = true,
            isCompleted = false,
            onStartPause = { toggleTimerPause() },
            onReset = { resetTimer() },
            onAddMinute = { addTimerSeconds(60) }
        )
        runTimerLoop()
    }

    fun toggleTimerPause() {
        val current = _timerState.value
        if (current.remainingSeconds <= 0) {
            resetTimer()
            return
        }
        if (current.isRunning) {
            timerJob?.cancel()
            _timerState.value = current.copy(isRunning = false)
        } else {
            _timerState.value = current.copy(isRunning = true)
            runTimerLoop()
        }
    }

    private fun runTimerLoop() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive && _timerState.value.isRunning && _timerState.value.remainingSeconds > 0) {
                delay(1000L)
                val current = _timerState.value
                val remaining = current.remainingSeconds - 1
                if (remaining <= 0) {
                    _timerState.value = current.copy(
                        remainingSeconds = 0,
                        isRunning = false,
                        isCompleted = true
                    )
                    break
                } else {
                    _timerState.value = current.copy(remainingSeconds = remaining)
                }
            }
        }
    }

    fun resetTimer() {
        timerJob?.cancel()
        val current = _timerState.value
        _timerState.value = current.copy(
            remainingSeconds = current.totalSeconds,
            isRunning = false,
            isCompleted = false
        )
    }

    fun stopTimer() {
        timerJob?.cancel()
        _timerState.value = TimerState()
    }

    fun addTimerSeconds(seconds: Int) {
        val current = _timerState.value
        val newTotal = current.totalSeconds + seconds
        val newRemaining = current.remainingSeconds + seconds
        _timerState.value = current.copy(
            totalSeconds = newTotal,
            remainingSeconds = newRemaining,
            isCompleted = false
        )
        if (!current.isRunning) {
            _timerState.value = _timerState.value.copy(isRunning = true)
            runTimerLoop()
        }
    }

    // --- STOPWATCH ---
    private val _stopwatchState = MutableStateFlow(StopwatchState())
    val stopwatchState: StateFlow<StopwatchState> = _stopwatchState
    private var stopwatchJob: Job? = null
    private var startTimeMillis = 0L
    private var accumulatedMillis = 0L

    fun startStopwatch() {
        if (_stopwatchState.value.isRunning) return
        startTimeMillis = System.currentTimeMillis()
        _stopwatchState.value = _stopwatchState.value.copy(
            isRunning = true,
            onStartPause = { toggleStopwatchPause() },
            onReset = { resetStopwatch() },
            onLap = { recordLap() }
        )
        runStopwatchLoop()
    }

    fun toggleStopwatchPause() {
        val current = _stopwatchState.value
        if (current.isRunning) {
            stopwatchJob?.cancel()
            accumulatedMillis += System.currentTimeMillis() - startTimeMillis
            _stopwatchState.value = current.copy(
                isRunning = false,
                elapsedMillis = accumulatedMillis
            )
        } else {
            startTimeMillis = System.currentTimeMillis()
            _stopwatchState.value = current.copy(isRunning = true)
            runStopwatchLoop()
        }
    }

    private fun runStopwatchLoop() {
        stopwatchJob?.cancel()
        stopwatchJob = scope.launch {
            while (isActive && _stopwatchState.value.isRunning) {
                delay(50L)
                val totalElapsed = accumulatedMillis + (System.currentTimeMillis() - startTimeMillis)
                _stopwatchState.value = _stopwatchState.value.copy(elapsedMillis = totalElapsed)
            }
        }
    }

    fun recordLap() {
        val current = _stopwatchState.value
        if (current.elapsedMillis > 0) {
            val newLaps = current.laps + current.elapsedMillis
            _stopwatchState.value = current.copy(laps = newLaps)
        }
    }

    fun resetStopwatch() {
        stopwatchJob?.cancel()
        accumulatedMillis = 0L
        startTimeMillis = 0L
        _stopwatchState.value = StopwatchState()
    }
}
