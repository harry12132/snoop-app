package com.harry12132.snoop.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import com.harry12132.snoop.data.OverlayAlignment
import com.harry12132.snoop.manager.SnoopStateHub
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Owns the one and only Snoop application-overlay window.
 * State and settings are observed here so the view never creates a second window.
 */
class SnoopOverlayController(
    context: Context,
    private val stateHub: SnoopStateHub
) {
    private val appContext = context.applicationContext
    private val windowManager =
        appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var overlayView: SnoopOverlayView? = null
    private var stateJob: Job? = null
    private var settingsJob: Job? = null
    private var params: WindowManager.LayoutParams? = null

    fun show() {
        if (overlayView != null) return

        val settings = stateHub.settingsManager.getSettings()
        val density = appContext.resources.displayMetrics.density
        val collapsedWidthPx = minOf(
            dp(settings.collapsedWidthDp, density),
            (appContext.resources.displayMetrics.widthPixels - dp(24, density)).coerceAtLeast(dp(120, density))
        )
        val windowParams = WindowManager.LayoutParams(
            collapsedWidthPx,
            dp(settings.collapsedHeightDp, density),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            val placement = computePlacement(settings, collapsedWidthPx, density)
            gravity = placement.horizontalGravity
            y = placement.y
            x = placement.x
        }

        val view = SnoopOverlayView(appContext, windowManager, windowParams, stateHub)
        view.updateSettings(settings)
        view.updateOverlayState(stateHub.overlayState.value)

        try {
            windowManager.addView(view, windowParams)
        } catch (error: RuntimeException) {
            view.dispose()
            throw error
        }
        params = windowParams
        overlayView = view

        stateJob = scope.launch {
            stateHub.overlayState.collectLatest { state ->
                overlayView?.updateOverlayState(state)
            }
        }
        settingsJob = scope.launch {
            stateHub.settingsManager.settingsFlow.collectLatest { updated ->
                overlayView?.updateSettings(updated)
                val view = overlayView ?: return@collectLatest
                val currentParams = params ?: return@collectLatest
                val widthPx = minOf(
                    dp(updated.collapsedWidthDp, density),
                    (appContext.resources.displayMetrics.widthPixels - dp(24, density)).coerceAtLeast(dp(120, density))
                )
                val placement = computePlacement(updated, widthPx, density)
                currentParams.gravity = placement.horizontalGravity
                currentParams.x = placement.x
                currentParams.y = placement.y
                try {
                    windowManager.updateViewLayout(view, currentParams)
                } catch (_: IllegalArgumentException) {
                    hide()
                } catch (_: WindowManager.BadTokenException) {
                    hide()
                }
            }
        }
    }

    /**
     * When auto-fit is enabled the window is placed from the REAL hardware
     * cutout so it can never sit on top of the camera. Otherwise the user's
     * manual alignment/offsets are used.
     */
    private fun computePlacement(
        settings: com.harry12132.snoop.data.SnoopSettings,
        collapsedWidthPx: Int,
        density: Float
    ): CutoutFitter.Placement {
        val yOffset = dp(settings.offsetYDp, density)
        return if (settings.autoFitToCutout) {
            CutoutFitter.placementFor(appContext, collapsedWidthPx, yOffset, dp(settings.offsetXDp, density))
        } else {
            CutoutFitter.Placement(gravityFor(settings.alignment), dp(settings.offsetXDp, density), yOffset)
        }
    }

    fun hide() {
        stateJob?.cancel()
        settingsJob?.cancel()
        stateJob = null
        settingsJob = null

        val view = overlayView
        overlayView = null
        params = null
        if (view != null) {
            view.dispose()
            try {
                windowManager.removeView(view)
            } catch (_: IllegalArgumentException) {
                // The system may already have detached the window.
            } catch (_: WindowManager.BadTokenException) {
                // The window token is no longer valid.
            }
        }
    }

    fun dispose() {
        hide()
        scope.cancel()
    }

    fun isShowing(): Boolean = overlayView?.windowToken != null

    private fun gravityFor(alignment: OverlayAlignment): Int {
        val horizontal = when (alignment) {
            OverlayAlignment.LEFT -> Gravity.LEFT
            OverlayAlignment.CENTER -> Gravity.CENTER_HORIZONTAL
            OverlayAlignment.RIGHT -> Gravity.RIGHT
        }
        return Gravity.TOP or horizontal
    }

    private fun dp(value: Int, density: Float): Int = (value * density).toInt()
}
