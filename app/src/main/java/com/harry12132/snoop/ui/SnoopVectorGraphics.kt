package com.harry12132.snoop.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.roundToInt

class EqualizerView(context: Context, private var isPlaying: Boolean = true) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(30, 215, 96)
        style = Paint.Style.FILL
    }

    private val barHeights = floatArrayOf(0.4f, 0.8f, 0.5f, 0.9f)
    private var animator: ValueAnimator? = null

    init {
        if (isPlaying) startAnimation()
    }

    fun setPlaying(playing: Boolean) {
        if (isPlaying == playing) return
        isPlaying = playing
        if (playing && isAttachedToWindow) {
            startAnimation()
        } else {
            animator?.cancel()
            animator = null
            barHeights[0] = 0.4f
            barHeights[1] = 0.65f
            barHeights[2] = 0.5f
            barHeights[3] = 0.75f
            invalidate()
        }
    }

    fun setBarColor(color: Int) {
        paint.color = color
        invalidate()
    }

    private fun startAnimation() {
        if (animator?.isRunning == true || !isPlaying) return
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 600L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                val fraction = animation.animatedFraction
                barHeights[0] = 0.3f + 0.6f * fraction
                barHeights[1] = 0.9f - 0.7f * fraction
                barHeights[2] = 0.4f + 0.5f * fraction
                barHeights[3] = 0.8f - 0.6f * fraction
                invalidate()
            }
            start()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isPlaying) startAnimation()
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val numBars = 4
        val padding = 4f
        val totalWidth = width.toFloat()
        val totalHeight = height.toFloat()
        val barWidth = (totalWidth - (numBars - 1) * padding) / numBars

        for (i in 0 until numBars) {
            val left = i * (barWidth + padding)
            val h = totalHeight * barHeights[i]
            val top = totalHeight - h
            val right = left + barWidth
            canvas.drawRoundRect(RectF(left, top, right, totalHeight), barWidth / 2f, barWidth / 2f, paint)
        }
    }
}
class ApplePlayPauseIconView(context: Context, var isPlaying: Boolean) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val path = Path()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        if (isPlaying) {
            val barW = w * 0.28f
            val spacing = w * 0.16f
            val left1 = (w - (2 * barW + spacing)) / 2f
            val top = h * 0.15f
            val bottom = h * 0.85f

            canvas.drawRoundRect(RectF(left1, top, left1 + barW, bottom), barW / 2f, barW / 2f, paint)
            canvas.drawRoundRect(RectF(left1 + barW + spacing, top, left1 + 2 * barW + spacing, bottom), barW / 2f, barW / 2f, paint)
        } else {
            path.reset()
            val left = w * 0.25f
            val top = h * 0.15f
            val bottom = h * 0.85f
            val right = w * 0.85f

            path.moveTo(left, top)
            path.lineTo(right, h / 2f)
            path.lineTo(left, bottom)
            path.close()
            canvas.drawPath(path, paint)
        }
    }
}

class AppleSkipIconView(context: Context, private val isNext: Boolean) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val path = Path()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        path.reset()
        if (isNext) {
            path.moveTo(w * 0.15f, h * 0.2f)
            path.lineTo(w * 0.55f, h * 0.5f)
            path.lineTo(w * 0.15f, h * 0.8f)
            path.close()

            path.moveTo(w * 0.45f, h * 0.2f)
            path.lineTo(w * 0.85f, h * 0.5f)
            path.lineTo(w * 0.45f, h * 0.8f)
            path.close()
        } else {
            path.moveTo(w * 0.85f, h * 0.2f)
            path.lineTo(w * 0.45f, h * 0.5f)
            path.lineTo(w * 0.85f, h * 0.8f)
            path.close()

            path.moveTo(w * 0.55f, h * 0.2f)
            path.lineTo(w * 0.15f, h * 0.5f)
            path.lineTo(w * 0.55f, h * 0.8f)
            path.close()
        }
        canvas.drawPath(path, paint)
    }
}

class AppleCallActionButton(
    context: Context,
    val isAnswer: Boolean,
    val onClickAction: () -> Unit
) : View(context) {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isAnswer) Color.rgb(52, 199, 89) else Color.rgb(255, 59, 48) // Apple green / red
        style = Paint.Style.FILL
    }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
    }

    init {
        setOnClickListener { onClickAction() }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val r = Math.min(cx, cy)

        canvas.drawCircle(cx, cy, r, bgPaint)

        // Draw phone handle arc / line
        iconPaint.strokeWidth = r * 0.2f
        val path = Path()
        if (isAnswer) {
            path.moveTo(cx - r * 0.35f, cy + r * 0.1f)
            path.quadTo(cx, cy - r * 0.35f, cx + r * 0.35f, cy + r * 0.1f)
        } else {
            path.moveTo(cx - r * 0.35f, cy - r * 0.1f)
            path.quadTo(cx, cy + r * 0.35f, cx + r * 0.35f, cy - r * 0.1f)
        }
        canvas.drawPath(path, iconPaint)
    }
}
