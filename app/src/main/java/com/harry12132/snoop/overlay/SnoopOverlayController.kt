package com.harry12132.snoop.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.harry12132.snoop.island.SnoopIslandOverlay
import com.harry12132.snoop.manager.SnoopStateHub

class SnoopOverlayController(
    context: Context,
    private val stateHub: SnoopStateHub
) {
    private val appContext = context.applicationContext
    private val windowManager =
        appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var composeView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    fun show() {
        if (composeView != null) return

        val lifecycle = OverlayLifecycleOwner()
        lifecycle.onCreate()
        lifecycleOwner = lifecycle

        val view = ComposeView(appContext).apply {
            setViewTreeLifecycleOwner(lifecycle)
            setViewTreeSavedStateRegistryOwner(lifecycle)
            setViewTreeViewModelStoreOwner(lifecycle)
            setContent {
                SnoopIslandOverlay(
                    stateHub = stateHub,
                    settingsManager = stateHub.settingsManager
                )
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = 0
        }

        try {
            windowManager.addView(view, params)
            composeView = view
        } catch (error: Exception) {
            lifecycleOwner?.onDestroy()
            lifecycleOwner = null
            composeView = null
            throw error
        }
    }

    fun hide() {
        val view = composeView ?: return
        composeView = null
        try {
            windowManager.removeView(view)
        } catch (_: IllegalArgumentException) {
        }
        lifecycleOwner?.onDestroy()
        lifecycleOwner = null
    }

    fun isShowing(): Boolean = composeView?.windowToken != null
}
