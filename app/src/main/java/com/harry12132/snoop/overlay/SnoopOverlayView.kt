package com.harry12132.snoop.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.view.WindowManager
import kotlin.math.roundToInt

/**
 * A small, self-contained overlay view.
 *
 * The window stays attached while its dimensions and corner radius animate
 * between collapsed and expanded states.
 */
class SnoopOverlayView(
    context: Context,
    private val windowManager: WindowManager,
    private val windowParams: WindowManager.LayoutParams
) : FrameLayout(context) {

    private val density = resources.displayMetrics.density
    private val collapsedWidth = dp(126)
    private val collapsedHeight = dp(36)
    private val expandedWidth = dp(320)
    private val expandedHeight = dp(164)
    private val pillBackground = GradientDrawable().apply {
        setColor(Color.rgb(8, 9, 11))
        cornerRadius = collapsedHeight / 2f
    }

    private var expanded = false
    private var animator: ValueAnimator? = null

    private val title = TextView(context).apply {
        text = "snoop"
        setTextColor(Color.WHITE)
        textSize = 13f
        gravity = Gravity.CENTER
        letterSpacing = 0.04f
    }

    init {
        background = pillBackground
        elevation = dp(12).toFloat()
        isClickable = true
        isFocusable = false
        clipToOutline = true

        addView(
            title,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )

        setOnClickListener { toggleExpanded() }
    }

    fun dispose() {
        animator?.cancel()
        animator = null
    }

    fun toggleExpanded() {
        animateTo(!expanded)
    }

    private fun animateTo(targetExpanded: Boolean) {
        if (animator?.isRunning == true) animator?.cancel()
        expanded = targetExpanded

        val startWidth = layoutParams?.width ?: collapsedWidth
        val startHeight = layoutParams?.height ?: collapsedHeight
        val endWidth = if (targetExpanded) expandedWidth else collapsedWidth
        val endHeight = if (targetExpanded) expandedHeight else collapsedHeight
        val startRadius = if (targetExpanded) collapsedHeight / 2f else dp(26).toFloat()
        val endRadius = if (targetExpanded) dp(26).toFloat() else collapsedHeight / 2f

        if (targetExpanded) showExpandedContent() else showCollapsedContent()

        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 320L
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float
                val params = layoutParams ?: return@addUpdateListener
                params.width = (startWidth + (endWidth - startWidth) * progress).roundToInt()
                params.height = (startHeight + (endHeight - startHeight) * progress).roundToInt()
                layoutParams = params
                pillBackground.cornerRadius = startRadius + (endRadius - startRadius) * progress
                invalidate()
                try {
                    windowManager.updateViewLayout(this@SnoopOverlayView, windowParams.apply {
                        width = params.width
                        height = params.height
                    })
                } catch (_: IllegalArgumentException) {
                    animator?.cancel()
                }
            }
            start()
        }
    }

    private fun showCollapsedContent() {
        removeAllViews()
        addView(title, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    private fun showExpandedContent() {
        removeAllViews()
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(22), dp(18), dp(22), dp(18))
        }

        val heading = TextView(context).apply {
            text = "snoop"
            setTextColor(Color.WHITE)
            textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        val description = TextView(context).apply {
            text = "Your glanceable space"
            setTextColor(Color.rgb(205, 209, 216))
            textSize = 13f
            setPadding(0, dp(8), 0, dp(18))
        }
        val collapse = TextView(context).apply {
            text = "Tap anywhere to close"
            setTextColor(Color.rgb(133, 141, 152))
            textSize = 12f
        }

        content.addView(heading)
        content.addView(description)
        content.addView(collapse)
        addView(content, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    private fun dp(value: Int): Int = (value * density).roundToInt()
}
