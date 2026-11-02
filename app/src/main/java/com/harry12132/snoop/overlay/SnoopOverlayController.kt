package com.harry12132.snoop.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.view.View

/**
 * Owns the single overlay window and guarantees it is attached/removed once.
 */
class SnoopOverlayController(context: Context) {
    private val appContext = context.applicationContext
    private val windowManager =
        appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var overlayView: SnoopOverlayView? = null

    fun show() {
        if (overlayView != null) return

        val statusBarHeight = appContext.resources.getIdentifier(
            "status_bar_height",
            "dimen",
            "android"
        ).takeIf { it != 0 }?.let { appContext.resources.getDimensionPixelSize(it) }
            ?: (24 * appContext.resources.displayMetrics.density).toInt()

        val width = (126 * appContext.resources.displayMetrics.density).toInt()
        val height = (36 * appContext.resources.displayMetrics.density).toInt()

        val params = LayoutParams(
            width,
            height,
            LayoutParams.TYPE_APPLICATION_OVERLAY,
            LayoutParams.FLAG_NOT_FOCUSABLE or LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = statusBarHeight + (6 * appContext.resources.displayMetrics.density).toInt()
            horizontalMargin = 0f
            verticalMargin = 0f
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
        try {
            windowManager.removeView(view)
        } catch (_: IllegalArgumentException) {
            // The system may already have detached the window.
        }
    }

    fun isShowing(): Boolean = overlayView?.windowToken != null
}
