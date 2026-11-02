package com.harry12132.snoop.model

import android.app.PendingIntent
import android.graphics.Bitmap

enum class ModuleType {
    IDLE,
    MUSIC,
    CALL,
    TIMER,
    STOPWATCH,
    NAVIGATION,
    NOTIFICATION,
    BATTERY,
    DOWNLOAD,
    SYSTEM_CONTROL
}

enum class CallStatus {
    INCOMING, OUTGOING, ACTIVE, ENDED
}

data class MusicState(
    val title: String = "No Track",
    val artist: String = "Unknown Artist",
    val album: String = "",
    val artwork: Bitmap? = null,
    val isPlaying: Boolean = false,
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val packageName: String = "",
    val onPlayPause: (() -> Unit)? = null,
    val onSkipNext: (() -> Unit)? = null,
    val onSkipPrev: (() -> Unit)? = null
)

data class CallState(
    val callerName: String = "Unknown",
    val callerNumber: String = "",
    val status: CallStatus = CallStatus.INCOMING,
    val durationSeconds: Int = 0,
    val onAnswer: (() -> Unit)? = null,
    val onReject: (() -> Unit)? = null,
    val onEndCall: (() -> Unit)? = null,
    val onMute: (() -> Unit)? = null
)

data class TimerState(
    val label: String = "Timer",
    val totalSeconds: Int = 300,
    val remainingSeconds: Int = 300,
    val isRunning: Boolean = false,
    val isCompleted: Boolean = false,
    val onStartPause: (() -> Unit)? = null,
    val onReset: (() -> Unit)? = null,
    val onAddMinute: (() -> Unit)? = null
)

data class StopwatchState(
    val elapsedMillis: Long = 0L,
    val isRunning: Boolean = false,
    val laps: List<Long> = emptyList(),
    val onStartPause: (() -> Unit)? = null,
    val onReset: (() -> Unit)? = null,
    val onLap: (() -> Unit)? = null
)

data class NavigationState(
    val appName: String = "",
    val instruction: String = "",
    val distanceText: String = "",
    val etaText: String = "",
    val iconBitmap: Bitmap? = null
)

data class NotificationActionItem(
    val title: String,
    val intent: PendingIntent? = null,
    val onClick: (() -> Unit)? = null
)

data class NotificationState(
    val id: String = "",
    val packageName: String = "",
    val appName: String = "Notification",
    val appIcon: Bitmap? = null,
    val title: String = "",
    val text: String = "",
    val postTime: Long = System.currentTimeMillis(),
    val actions: List<NotificationActionItem> = emptyList(),
    val onDismiss: (() -> Unit)? = null
)

data class BatteryState(
    val percentage: Int = 100,
    val isCharging: Boolean = false,
    val plugType: String = "Unplugged",
    val isLowBattery: Boolean = false
)

data class DownloadState(
    val downloadId: String = "",
    val fileName: String = "",
    val percentage: Int = 0,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val isCompleted: Boolean = false
)

enum class RingerMode {
    NORMAL, VIBRATE, SILENT
}

data class SystemControlState(
    val isFlashlightOn: Boolean = false,
    val ringerMode: RingerMode = RingerMode.NORMAL,
    val volumePercent: Int = 50,
    val onToggleFlashlight: (() -> Unit)? = null,
    val onToggleRinger: (() -> Unit)? = null
)

data class SnoopOverlayState(
    val activeModule: ModuleType = ModuleType.IDLE,
    val availableModules: List<ModuleType> = listOf(ModuleType.IDLE),
    val music: MusicState = MusicState(),
    val call: CallState = CallState(),
    val timer: TimerState = TimerState(),
    val stopwatch: StopwatchState = StopwatchState(),
    val navigation: NavigationState = NavigationState(),
    val notification: NotificationState = NotificationState(),
    val battery: BatteryState = BatteryState(),
    val download: DownloadState = DownloadState(),
    val systemControl: SystemControlState = SystemControlState()
)
