package com.harry12132.snoop.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class OverlayAlignment {
    LEFT, CENTER, RIGHT
}

enum class CutoutMode {
    PUNCH_HOLE_CENTER,
    PUNCH_HOLE_LEFT,
    PUNCH_HOLE_RIGHT,
    WATERDROP_NOTCH,
    CUSTOM
}

data class SnoopSettings(
    val alignment: OverlayAlignment = OverlayAlignment.CENTER,
    val cutoutMode: CutoutMode = CutoutMode.PUNCH_HOLE_CENTER,
    val offsetYDp: Int = 12,
    val offsetXDp: Int = 0,
    val collapsedWidthDp: Int = 150,
    val collapsedHeightDp: Int = 36,
    val expandedWidthDp: Int = 340,
    val expandedHeightDp: Int = 190,
    val cornerRadiusDp: Int = 28,
    val backgroundOpacity: Float = 0.98f,
    val showSecondaryBubble: Boolean = false,
    val showAlignmentGuide: Boolean = false,
    val enableMusic: Boolean = true,
    val enableCalls: Boolean = true,
    val enableTimers: Boolean = true,
    val enableStopwatches: Boolean = true,
    val enableNavigation: Boolean = true,
    val enableNotifications: Boolean = true,
    val enableBattery: Boolean = true,
    val enableDownloads: Boolean = true,
    val enableSystemControls: Boolean = true,
    val enableSwipeGestures: Boolean = true,
    val autoCollapseSeconds: Int = 5
)

class SnoopSettingsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<SnoopSettings> = _settingsFlow

    fun getSettings(): SnoopSettings = _settingsFlow.value

    fun updateSettings(transform: (SnoopSettings) -> SnoopSettings) {
        val updated = transform(_settingsFlow.value)
        saveSettings(updated)
        _settingsFlow.value = updated
    }

    private fun loadSettings(): SnoopSettings {
        val alignmentName = prefs.getString(KEY_ALIGNMENT, OverlayAlignment.CENTER.name)
        val alignment = try {
            OverlayAlignment.valueOf(alignmentName ?: OverlayAlignment.CENTER.name)
        } catch (_: Exception) {
            OverlayAlignment.CENTER
        }

        val cutoutName = prefs.getString(KEY_CUTOUT_MODE, CutoutMode.PUNCH_HOLE_CENTER.name)
        val cutoutMode = try {
            CutoutMode.valueOf(cutoutName ?: CutoutMode.PUNCH_HOLE_CENTER.name)
        } catch (_: Exception) {
            CutoutMode.PUNCH_HOLE_CENTER
        }

        return SnoopSettings(
            alignment = alignment,
            cutoutMode = cutoutMode,
            offsetYDp = prefs.getInt(KEY_OFFSET_Y, 12),
            offsetXDp = prefs.getInt(KEY_OFFSET_X, 0),
            collapsedWidthDp = prefs.getInt(KEY_COLLAPSED_W, 150),
            collapsedHeightDp = prefs.getInt(KEY_COLLAPSED_H, 36),
            expandedWidthDp = prefs.getInt(KEY_EXPANDED_W, 340),
            expandedHeightDp = prefs.getInt(KEY_EXPANDED_H, 190),
            cornerRadiusDp = prefs.getInt(KEY_CORNER_RADIUS, 28),
            backgroundOpacity = prefs.getFloat(KEY_OPACITY, 0.98f),
            showSecondaryBubble = prefs.getBoolean(KEY_SECONDARY_BUBBLE, false),
            showAlignmentGuide = prefs.getBoolean(KEY_ALIGNMENT_GUIDE, false),
            enableMusic = prefs.getBoolean(KEY_ENABLE_MUSIC, true),
            enableCalls = prefs.getBoolean(KEY_ENABLE_CALLS, true),
            enableTimers = prefs.getBoolean(KEY_ENABLE_TIMERS, true),
            enableStopwatches = prefs.getBoolean(KEY_ENABLE_STOPWATCHES, true),
            enableNavigation = prefs.getBoolean(KEY_ENABLE_NAV, true),
            enableNotifications = prefs.getBoolean(KEY_ENABLE_NOTIF, true),
            enableBattery = prefs.getBoolean(KEY_ENABLE_BATTERY, true),
            enableDownloads = prefs.getBoolean(KEY_ENABLE_DOWNLOADS, true),
            enableSystemControls = prefs.getBoolean(KEY_ENABLE_SYS_CTRL, true),
            enableSwipeGestures = prefs.getBoolean(KEY_ENABLE_SWIPE, true),
            autoCollapseSeconds = prefs.getInt(KEY_AUTO_COLLAPSE, 5)
        )
    }

    private fun saveSettings(settings: SnoopSettings) {
        prefs.edit()
            .putString(KEY_ALIGNMENT, settings.alignment.name)
            .putString(KEY_CUTOUT_MODE, settings.cutoutMode.name)
            .putInt(KEY_OFFSET_Y, settings.offsetYDp)
            .putInt(KEY_OFFSET_X, settings.offsetXDp)
            .putInt(KEY_COLLAPSED_W, settings.collapsedWidthDp)
            .putInt(KEY_COLLAPSED_H, settings.collapsedHeightDp)
            .putInt(KEY_EXPANDED_W, settings.expandedWidthDp)
            .putInt(KEY_EXPANDED_H, settings.expandedHeightDp)
            .putInt(KEY_CORNER_RADIUS, settings.cornerRadiusDp)
            .putFloat(KEY_OPACITY, settings.backgroundOpacity)
            .putBoolean(KEY_SECONDARY_BUBBLE, settings.showSecondaryBubble)
            .putBoolean(KEY_ALIGNMENT_GUIDE, settings.showAlignmentGuide)
            .putBoolean(KEY_ENABLE_MUSIC, settings.enableMusic)
            .putBoolean(KEY_ENABLE_CALLS, settings.enableCalls)
            .putBoolean(KEY_ENABLE_TIMERS, settings.enableTimers)
            .putBoolean(KEY_ENABLE_STOPWATCHES, settings.enableStopwatches)
            .putBoolean(KEY_ENABLE_NAV, settings.enableNavigation)
            .putBoolean(KEY_ENABLE_NOTIF, settings.enableNotifications)
            .putBoolean(KEY_ENABLE_BATTERY, settings.enableBattery)
            .putBoolean(KEY_ENABLE_DOWNLOADS, settings.enableDownloads)
            .putBoolean(KEY_ENABLE_SYS_CTRL, settings.enableSystemControls)
            .putBoolean(KEY_ENABLE_SWIPE, settings.enableSwipeGestures)
            .putInt(KEY_AUTO_COLLAPSE, settings.autoCollapseSeconds)
            .apply()
    }

    companion object {
        private const val PREF_NAME = "snoop_settings_prefs"
        private const val KEY_ALIGNMENT = "alignment"
        private const val KEY_CUTOUT_MODE = "cutout_mode"
        private const val KEY_OFFSET_Y = "offset_y"
        private const val KEY_OFFSET_X = "offset_x"
        private const val KEY_COLLAPSED_W = "collapsed_w"
        private const val KEY_COLLAPSED_H = "collapsed_h"
        private const val KEY_EXPANDED_W = "expanded_w"
        private const val KEY_EXPANDED_H = "expanded_h"
        private const val KEY_CORNER_RADIUS = "corner_radius"
        private const val KEY_OPACITY = "opacity"
        private const val KEY_SECONDARY_BUBBLE = "secondary_bubble"
        private const val KEY_ALIGNMENT_GUIDE = "alignment_guide"
        private const val KEY_ENABLE_MUSIC = "enable_music"
        private const val KEY_ENABLE_CALLS = "enable_calls"
        private const val KEY_ENABLE_TIMERS = "enable_timers"
        private const val KEY_ENABLE_STOPWATCHES = "enable_stopwatches"
        private const val KEY_ENABLE_NAV = "enable_nav"
        private const val KEY_ENABLE_NOTIF = "enable_notif"
        private const val KEY_ENABLE_BATTERY = "enable_battery"
        private const val KEY_ENABLE_DOWNLOADS = "enable_downloads"
        private const val KEY_ENABLE_SYS_CTRL = "enable_sys_ctrl"
        private const val KEY_ENABLE_SWIPE = "enable_swipe"
        private const val KEY_AUTO_COLLAPSE = "auto_collapse"
    }
}
