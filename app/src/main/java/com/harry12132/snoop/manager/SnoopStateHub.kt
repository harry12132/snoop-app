package com.harry12132.snoop.manager

import android.content.Context
import com.harry12132.snoop.data.SnoopSettings
import com.harry12132.snoop.data.SnoopSettingsManager
import com.harry12132.snoop.engine.TimerStopwatchEngine
import com.harry12132.snoop.model.BatteryState
import com.harry12132.snoop.model.CallState
import com.harry12132.snoop.model.CallStatus
import com.harry12132.snoop.model.DownloadState
import com.harry12132.snoop.model.ModuleType
import com.harry12132.snoop.model.MusicState
import com.harry12132.snoop.model.NavigationState
import com.harry12132.snoop.model.NotificationState
import com.harry12132.snoop.model.SnoopOverlayState
import com.harry12132.snoop.model.StopwatchState
import com.harry12132.snoop.model.SystemControlState
import com.harry12132.snoop.model.TimerState
import com.harry12132.snoop.observer.BatteryObserver
import com.harry12132.snoop.observer.CallObserver
import com.harry12132.snoop.service.SnoopNotificationListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SnoopStateHub(
    private val context: Context,
    val settingsManager: SnoopSettingsManager
) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private var hubJob: Job? = null

    val batteryObserver = BatteryObserver(context)
    val callObserver = CallObserver(context)
    val timerStopwatchEngine = TimerStopwatchEngine()
    val systemControlsManager = SystemControlsManager(context)

    private val _overlayState = MutableStateFlow(SnoopOverlayState())
    val overlayState: StateFlow<SnoopOverlayState> = _overlayState

    private var userSelectedModuleIndex = 0

    fun start() {
        if (hubJob?.isActive == true) return
        batteryObserver.start()
        callObserver.start()

        hubJob = scope.launch {
            launch { observeSettings() }
            launch { observeBattery() }
            launch { observeCalls() }
            launch { observeTimers() }
            launch { observeStopwatch() }
            launch { observeSystemControls() }
            launch { observeNotificationListenerService() }
            launch { periodicMediaCheck() }
        }
    }

    fun refreshCallPermission() {
        callObserver.stop()
        callObserver.start()
    }

    fun stop() {
        hubJob?.cancel()
        batteryObserver.stop()
        callObserver.stop()
    }

    private suspend fun observeSettings() {
        settingsManager.settingsFlow.collectLatest {
            recalculateState()
        }
    }

    private suspend fun observeBattery() {
        batteryObserver.batteryState.collectLatest {
            recalculateState()
        }
    }

    private suspend fun observeCalls() {
        callObserver.callState.collectLatest {
            recalculateState()
        }
    }

    private suspend fun observeTimers() {
        timerStopwatchEngine.timerState.collectLatest {
            recalculateState()
        }
    }

    private suspend fun observeStopwatch() {
        timerStopwatchEngine.stopwatchState.collectLatest {
            recalculateState()
        }
    }

    private suspend fun observeSystemControls() {
        systemControlsManager.systemState.collectLatest {
            recalculateState()
        }
    }

    private suspend fun observeNotificationListenerService() = kotlinx.coroutines.coroutineScope {
        launch {
            SnoopNotificationListenerService.latestMusicFlow.collectLatest {
                recalculateState()
            }
        }
        launch {
            SnoopNotificationListenerService.latestNavigationFlow.collectLatest {
                recalculateState()
            }
        }
        launch {
            SnoopNotificationListenerService.latestNotificationFlow.collectLatest {
                recalculateState()
            }
        }
        launch {
            SnoopNotificationListenerService.latestDownloadFlow.collectLatest {
                recalculateState()
            }
        }
    }

    private suspend fun periodicMediaCheck() {
        while (true) {
            SnoopNotificationListenerService.updateMediaSessions(context)
            delay(2000L)
        }
    }

    fun cycleNextModule() {
        val currentAvail = _overlayState.value.availableModules
        if (currentAvail.size <= 1) return
        userSelectedModuleIndex = (userSelectedModuleIndex + 1) % currentAvail.size
        val nextMod = currentAvail[userSelectedModuleIndex]
        _overlayState.value = _overlayState.value.copy(activeModule = nextMod)
    }

    fun cyclePrevModule() {
        val currentAvail = _overlayState.value.availableModules
        if (currentAvail.size <= 1) return
        userSelectedModuleIndex = if (userSelectedModuleIndex - 1 < 0) {
            currentAvail.size - 1
        } else {
            userSelectedModuleIndex - 1
        }
        val prevMod = currentAvail[userSelectedModuleIndex]
        _overlayState.value = _overlayState.value.copy(activeModule = prevMod)
    }

    private fun recalculateState() {
        val settings = settingsManager.getSettings()

        val activeCall = callObserver.callState.value
        val timer = timerStopwatchEngine.timerState.value
        val stopwatch = timerStopwatchEngine.stopwatchState.value
        val music = SnoopNotificationListenerService.latestMusicFlow.value ?: MusicState()
        val nav = SnoopNotificationListenerService.latestNavigationFlow.value ?: NavigationState()
        val notif = SnoopNotificationListenerService.latestNotificationFlow.value ?: NotificationState()
        val download = SnoopNotificationListenerService.latestDownloadFlow.value ?: DownloadState()
        val battery = batteryObserver.batteryState.value
        val sysCtrl = systemControlsManager.systemState.value

        val available = mutableListOf<ModuleType>()

        if (settings.enableCalls && activeCall != null) available.add(ModuleType.CALL)
        if (settings.enableTimers && (timer.isRunning || timer.isCompleted)) available.add(ModuleType.TIMER)
        if (settings.enableStopwatches && stopwatch.isRunning) available.add(ModuleType.STOPWATCH)
        if (settings.enableNavigation && nav.instruction.isNotBlank() && nav.distanceText != "Navigating") available.add(ModuleType.NAVIGATION)
        if (settings.enableMusic && (music.isPlaying || music.title != "No Track")) available.add(ModuleType.MUSIC)
        if (settings.enableNotifications && notif.title.isNotBlank()) available.add(ModuleType.NOTIFICATION)
        if (settings.enableDownloads && download.fileName.isNotBlank() && !download.isCompleted) available.add(ModuleType.DOWNLOAD)
        if (settings.enableBattery && (battery.isCharging || battery.isLowBattery)) available.add(ModuleType.BATTERY)
        if (settings.enableSystemControls && (sysCtrl.isFlashlightOn || sysCtrl.ringerMode != com.harry12132.snoop.model.RingerMode.NORMAL)) {
            available.add(ModuleType.SYSTEM_CONTROL)
        }

        if (available.isEmpty()) {
            available.add(ModuleType.IDLE)
        }

        if (userSelectedModuleIndex >= available.size) {
            userSelectedModuleIndex = 0
        }

        val primaryModule = available.firstOrNull() ?: ModuleType.IDLE

        _overlayState.value = SnoopOverlayState(
            activeModule = available.getOrElse(userSelectedModuleIndex) { primaryModule },
            availableModules = available,
            music = music,
            call = activeCall ?: CallState(),
            timer = timer,
            stopwatch = stopwatch,
            navigation = nav,
            notification = notif,
            battery = battery,
            download = download,
            systemControl = sysCtrl
        )
    }

    // --- SIMULATION HELPERS FOR TESTING ---
    fun simulateCall(status: CallStatus, name: String = "Jane Doe") {
        callObserver.setSimulatedCall(status, name)
    }

    fun simulateMusic(isPlaying: Boolean, title: String = "Blinding Lights", artist: String = "The Weeknd") {
        SnoopNotificationListenerService.updateMediaSessions(context)
    }

    fun startTimer(seconds: Int = 180, label: String = "Tea Timer") {
        timerStopwatchEngine.startTimer(seconds, label)
    }

    fun startStopwatch() {
        timerStopwatchEngine.startStopwatch()
    }

    fun toggleFlashlight() {
        systemControlsManager.toggleFlashlight()
    }
}
