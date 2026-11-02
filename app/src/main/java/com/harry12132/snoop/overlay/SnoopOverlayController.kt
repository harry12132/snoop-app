package com.harry12132.snoop.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager

/** Owns the single overlay window and guarantees it is attached/removed once. */
class SnoopOverlayController(context: Context) {
    private val appContext = context.applicationContext
    private val windowManager =
        appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var overlayView: SnoopOverlayView? = null

    fun show() {
        if (overlayView != null) return

        val density = appContext.resources.displayMetrics.density
        val topOffsetDp = appContext.getSharedPreferences("snoop_settings", Context.MODE_PRIVATE)
            .getInt("top_offset_dp", 16)
        val params = WindowManager.LayoutParams(
            (126 * density).toInt(),
            (36 * density).toInt(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = (topOffsetDp * density).toInt()
        }

        val view = SnoopOverlayView(appContext, windowManager, params)
        try {
            windowManager.addView(view, params)
            overlayView = view
        } catch (error: WindowManager.BadTokenException) {
            overlayView = null
            throw error
        } catch (error: SecurityException) {
            overlayView = null
            throw error
        }
    }

    fun hide() {
        val view = overlayView ?: return
        overlayView = null
        view.dispose()
        try {
            windowManager.removeView(view)
        } catch (_: IllegalArgumentException) {
            // The system may already have detached the window.
        }
    }

    fun isShowing(): Boolean = overlayView?.windowToken != null
}
