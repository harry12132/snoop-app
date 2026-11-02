package com.harry12132.snoop.overlay

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.harry12132.snoop.data.SnoopSettings
import com.harry12132.snoop.manager.SnoopStateHub
import com.harry12132.snoop.model.CallStatus
import com.harry12132.snoop.model.ModuleType
import com.harry12132.snoop.model.RingerMode
import com.harry12132.snoop.model.SnoopOverlayState
import com.harry12132.snoop.ui.ApplePlayPauseIconView
import com.harry12132.snoop.ui.AppleSkipIconView
import com.harry12132.snoop.ui.EqualizerView
import kotlin.math.abs
import kotlin.math.roundToInt

@SuppressLint("ViewConstructor")
class SnoopOverlayView(
    context: Context,
    private val windowManager: WindowManager,
    private val windowParams: WindowManager.LayoutParams,
    private val stateHub: SnoopStateHub
) : FrameLayout(context) {

    private val density = resources.displayMetrics.density

    private var currentSettings = stateHub.settingsManager.getSettings()
    private var currentState = stateHub.overlayState.value

    private val mainPillBackground = GradientDrawable().apply {
        setColor(Color.BLACK)
        cornerRadius = dp(currentSettings.collapsedHeightDp / 2).toFloat()
        if (currentSettings.showAlignmentGuide) {
            setStroke(dp(2), Color.argb(220, 255, 59, 48))
        }
    }

    private var isExpanded = false
    private var isDraggingPosition = false
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var initialParamX = 0
    private var initialParamY = 0

    private var animator: ValueAnimator? = null

    private val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                toggleExpanded()
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                isDraggingPosition = true
                mainPillBackground.setStroke(dp(2), Color.rgb(52, 199, 89))
                invalidate()
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y

                if (abs(diffX) > abs(diffY)) {
                    if (currentSettings.enableSwipeGestures && abs(diffX) > 80 && abs(velocityX) > 80) {
                        if (diffX > 0) {
                            stateHub.cycleNextModule()
                        } else {
                            stateHub.cyclePrevModule()
                        }
                        return true
                    }
                } else if (diffY < -80 && abs(velocityY) > 80) {
                    if (isExpanded) {
                        animateTo(false)
                        return true
                    }
                }
                return false
            }
        },
        Handler(Looper.getMainLooper())
    )

    init {
        background = mainPillBackground
        elevation = dp(16).toFloat()
        isClickable = true
        isFocusable = false
        clipToOutline = true

        renderUI()
    }

    fun updateSettings(settings: SnoopSettings) {
        currentSettings = settings
        mainPillBackground.setColor(Color.argb((settings.backgroundOpacity * 255).toInt(), 0, 0, 0))
        if (settings.showAlignmentGuide) {
            mainPillBackground.setStroke(dp(2), Color.argb(220, 255, 59, 48))
        } else {
            mainPillBackground.setStroke(0, Color.TRANSPARENT)
        }
        mainPillBackground.cornerRadius = if (isExpanded) dp(settings.cornerRadiusDp).toFloat() else dp(settings.collapsedHeightDp / 2).toFloat()
        invalidate()
    }

    fun updateOverlayState(state: SnoopOverlayState) {
        currentState = state
        renderUI()
    }

    fun dispose() {
        animator?.cancel()
        animator = null
    }

    fun toggleExpanded() {
        animateTo(!isExpanded)
    }

    private fun animateTo(targetExpanded: Boolean) {
        if (animator?.isRunning == true) animator?.cancel()
        isExpanded = targetExpanded

        val collapsedWidth = dp(currentSettings.collapsedWidthDp)
        val collapsedHeight = dp(currentSettings.collapsedHeightDp)
        val expandedWidth = dp(currentSettings.expandedWidthDp)
        val expandedHeight = dp(currentSettings.expandedHeightDp)

        val startWidth = layoutParams?.width ?: collapsedWidth
        val startHeight = layoutParams?.height ?: collapsedHeight
        val endWidth = if (targetExpanded) expandedWidth else collapsedWidth
        val endHeight = if (targetExpanded) expandedHeight else collapsedHeight

        val startRadius = if (targetExpanded) collapsedHeight / 2f else dp(currentSettings.cornerRadiusDp).toFloat()
        val endRadius = if (targetExpanded) dp(currentSettings.cornerRadiusDp).toFloat() else collapsedHeight / 2f

        renderUI()

        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 340L
            interpolator = OvershootInterpolator(0.7f)
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float
                val params = layoutParams ?: return@addUpdateListener
                params.width = (startWidth + (endWidth - startWidth) * progress).roundToInt()
                params.height = (startHeight + (endHeight - startHeight) * progress).roundToInt()
                layoutParams = params
                mainPillBackground.cornerRadius = startRadius + (endRadius - startRadius) * progress
                invalidate()
                try {
                    windowManager.updateViewLayout(this@SnoopOverlayView, windowParams.apply {
                        width = params.width
                        height = params.height
                    })
                } catch (_: Exception) {
                    animator?.cancel()
                }
            }
            start()
        }
    }

    private fun renderUI() {
        removeAllViews()
        if (isExpanded) {
            renderExpandedView()
        } else {
            renderCollapsedView()
        }
    }

    private fun renderCollapsedView() {
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), 0, dp(12), 0)
        }

        // Left Side: Artwork or Icon
        val leftView = buildCollapsedLeftElement()

        // Middle Title / Status
        val middleText = TextView(context).apply {
            text = getCollapsedTitle()
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 1
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(dp(8), 0, dp(8), 0)
            }
        }

        // Right Side: Animated Equalizer or Timer Ring or Battery
        val rightView = buildCollapsedRightElement()

        root.addView(leftView)
        root.addView(middleText)
        if (rightView != null) {
            root.addView(rightView)
        }

        addView(root, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    private fun buildCollapsedLeftElement(): View {
        return when (currentState.activeModule) {
            ModuleType.MUSIC -> {
                ImageView(context).apply {
                    layoutParams = LayoutParams(dp(20), dp(20))
                    if (currentState.music.artwork != null) {
                        setImageBitmap(currentState.music.artwork)
                    } else {
                        setBackgroundColor(Color.rgb(40, 40, 45))
                    }
                    clipToOutline = true
                }
            }
            ModuleType.CALL -> {
                TextView(context).apply {
                    text = "🟢"
                    textSize = 12f
                }
            }
            ModuleType.TIMER -> {
                TextView(context).apply {
                    text = "⏱️"
                    textSize = 12f
                }
            }
            else -> {
                View(context).apply {
                    layoutParams = LayoutParams(dp(8), dp(8))
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.rgb(255, 159, 10))
                    }
                }
            }
        }
    }

    private fun buildCollapsedRightElement(): View? {
        return when (currentState.activeModule) {
            ModuleType.MUSIC -> {
                if (currentState.music.isPlaying) {
                    EqualizerView(context).apply {
                        layoutParams = LayoutParams(dp(18), dp(14))
                    }
                } else null
            }
            ModuleType.TIMER -> {
                val rem = currentState.timer.remainingSeconds
                TextView(context).apply {
                    text = String.format("%02d:%02d", rem / 60, rem % 60)
                    setTextColor(Color.rgb(255, 159, 10))
                    textSize = 12f
                    typeface = Typeface.DEFAULT_BOLD
                }
            }
            ModuleType.BATTERY -> {
                TextView(context).apply {
                    text = "⚡${currentState.battery.percentage}%"
                    setTextColor(Color.rgb(52, 199, 89))
                    textSize = 12f
                    typeface = Typeface.DEFAULT_BOLD
                }
            }
            else -> null
        }
    }

    private fun renderExpandedView() {
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
        }

        // Header: Module Indicator & Swiping Dots
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val headerTitle = TextView(context).apply {
            text = getModuleHeaderTitle(currentState.activeModule)
            setTextColor(Color.rgb(142, 142, 147))
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.08f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val dotsIndicator = TextView(context).apply {
            val idx = currentState.availableModules.indexOf(currentState.activeModule) + 1
            text = "• $idx/${currentState.availableModules.size} •"
            setTextColor(Color.rgb(99, 99, 102))
            textSize = 11f
        }

        header.addView(headerTitle)
        header.addView(dotsIndicator)
        root.addView(header)

        // Main Content Container
        val contentContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply {
                setMargins(0, dp(10), 0, dp(6))
            }
        }

        when (currentState.activeModule) {
            ModuleType.MUSIC -> buildMusicViewExpanded(contentContainer)
            ModuleType.CALL -> buildCallViewExpanded(contentContainer)
            ModuleType.TIMER -> buildTimerViewExpanded(contentContainer)
            ModuleType.STOPWATCH -> buildStopwatchViewExpanded(contentContainer)
            ModuleType.NAVIGATION -> buildNavigationViewExpanded(contentContainer)
            ModuleType.NOTIFICATION -> buildNotificationViewExpanded(contentContainer)
            ModuleType.BATTERY -> buildBatteryViewExpanded(contentContainer)
            ModuleType.DOWNLOAD -> buildDownloadViewExpanded(contentContainer)
            ModuleType.SYSTEM_CONTROL -> buildSystemControlViewExpanded(contentContainer)
            ModuleType.IDLE -> buildIdleViewExpanded(contentContainer)
        }

        root.addView(contentContainer)
        addView(root, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    private fun buildMusicViewExpanded(container: LinearLayout) {
        val music = currentState.music

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val artView = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(54), dp(54)).apply {
                setMargins(0, 0, dp(14), 0)
            }
            if (music.artwork != null) {
                setImageBitmap(music.artwork)
            } else {
                setBackgroundColor(Color.rgb(44, 44, 46))
            }
            clipToOutline = true
        }

        val textCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val trackText = TextView(context).apply {
            text = music.title
            setTextColor(Color.WHITE)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 1
        }
        val artistText = TextView(context).apply {
            text = music.artist
            setTextColor(Color.rgb(174, 174, 178))
            textSize = 14f
            maxLines = 1
        }

        textCol.addView(trackText)
        textCol.addView(artistText)

        val eqView = EqualizerView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(22), dp(18))
        }

        row.addView(artView)
        row.addView(textCol)
        row.addView(eqView)

        // Progress Bar
        val progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            val progressPct = if (music.durationMs > 0) ((music.positionMs * 100) / music.durationMs).toInt() else 35
            progress = progressPct
            max = 100
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(4)
            ).apply {
                setMargins(0, dp(12), 0, dp(12))
            }
        }

        // Apple Controls Row
        val controlsRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val prevIcon = AppleSkipIconView(context, isNext = false).apply {
            layoutParams = LinearLayout.LayoutParams(dp(28), dp(28)).apply {
                setMargins(dp(16), 0, dp(16), 0)
            }
            setOnClickListener { music.onSkipPrev?.invoke() }
        }

        val playPauseIcon = ApplePlayPauseIconView(context, isPlaying = music.isPlaying).apply {
            layoutParams = LinearLayout.LayoutParams(dp(36), dp(36)).apply {
                setMargins(dp(16), 0, dp(16), 0)
            }
            setOnClickListener { music.onPlayPause?.invoke() }
        }

        val nextIcon = AppleSkipIconView(context, isNext = true).apply {
            layoutParams = LinearLayout.LayoutParams(dp(28), dp(28)).apply {
                setMargins(dp(16), 0, dp(16), 0)
            }
            setOnClickListener { music.onSkipNext?.invoke() }
        }

        controlsRow.addView(prevIcon)
        controlsRow.addView(playPauseIcon)
        controlsRow.addView(nextIcon)

        container.addView(row)
        container.addView(progressBar)
        container.addView(controlsRow)
    }

    private fun buildCallViewExpanded(container: LinearLayout) {
        val call = currentState.call

        val callerText = TextView(context).apply {
            text = call.callerName
            setTextColor(Color.WHITE)
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val statusText = TextView(context).apply {
            val mins = call.durationSeconds / 60
            val secs = call.durationSeconds % 60
            val timeStr = String.format("%02d:%02d", mins, secs)
            text = when (call.status) {
                CallStatus.INCOMING -> "Incoming Call"
                CallStatus.OUTGOING -> "Calling..."
                CallStatus.ACTIVE -> "Active Call • $timeStr"
                CallStatus.ENDED -> "Call Ended"
            }
            setTextColor(Color.rgb(174, 174, 178))
            textSize = 14f
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val guidanceText = TextView(context).apply {
            text = when (call.status) {
                CallStatus.INCOMING -> "Answer or decline using your phone's call controls."
                CallStatus.OUTGOING, CallStatus.ACTIVE -> "Manage this call using your phone's call controls."
                CallStatus.ENDED -> "Call finished."
            }
            setTextColor(Color.rgb(142, 142, 147))
            textSize = 12f
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, dp(12), 0, 0)
        }

        // Android does not let an ordinary overlay app answer or hang up
        // cellular calls. Do not show decorative buttons that cannot work.
        container.addView(callerText)
        container.addView(statusText)
        container.addView(guidanceText)
    }

    private fun buildTimerViewExpanded(container: LinearLayout) {
        val timer = currentState.timer

        val mins = timer.remainingSeconds / 60
        val secs = timer.remainingSeconds % 60
        val timeStr = String.format("%02d:%02d", mins, secs)

        val timerText = TextView(context).apply {
            text = if (timer.isCompleted) "00:00" else timeStr
            setTextColor(if (timer.isCompleted) Color.rgb(255, 59, 48) else Color.rgb(255, 159, 10))
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val labelText = TextView(context).apply {
            text = timer.label
            setTextColor(Color.rgb(174, 174, 178))
            textSize = 14f
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val actionsRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, 0)
        }

        val toggleBtn = createAppleButton(if (timer.isRunning) "Pause" else "Start") {
            stateHub.timerStopwatchEngine.toggleTimerPause()
        }
        val resetBtn = createAppleButton("Reset") {
            stateHub.timerStopwatchEngine.resetTimer()
        }
        val add1mBtn = createAppleButton("+1m") {
            stateHub.timerStopwatchEngine.addTimerSeconds(60)
        }

        actionsRow.addView(toggleBtn)
        actionsRow.addView(resetBtn)
        actionsRow.addView(add1mBtn)

        container.addView(timerText)
        container.addView(labelText)
        container.addView(actionsRow)
    }

    private fun buildStopwatchViewExpanded(container: LinearLayout) {
        val sw = currentState.stopwatch

        val totalSecs = sw.elapsedMillis / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        val millis = (sw.elapsedMillis % 1000) / 10
        val timeStr = String.format("%02d:%02d.%02d", mins, secs, millis)

        val swText = TextView(context).apply {
            text = timeStr
            setTextColor(Color.WHITE)
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val actionsRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, 0)
        }

        val startPauseBtn = createAppleButton(if (sw.isRunning) "Pause" else "Start") {
            stateHub.timerStopwatchEngine.toggleStopwatchPause()
        }
        val lapBtn = createAppleButton("Lap") {
            stateHub.timerStopwatchEngine.recordLap()
        }
        val resetBtn = createAppleButton("Reset") {
            stateHub.timerStopwatchEngine.resetStopwatch()
        }

        actionsRow.addView(startPauseBtn)
        actionsRow.addView(lapBtn)
        actionsRow.addView(resetBtn)

        container.addView(swText)
        container.addView(actionsRow)
    }

    private fun buildNavigationViewExpanded(container: LinearLayout) {
        val nav = currentState.navigation

        val instructionText = TextView(context).apply {
            text = nav.instruction
            setTextColor(Color.WHITE)
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
        }

        val infoText = TextView(context).apply {
            text = "In ${nav.distanceText} • ETA ${nav.etaText}"
            setTextColor(Color.rgb(174, 174, 178))
            textSize = 14f
        }

        container.addView(instructionText)
        container.addView(infoText)
    }

    private fun buildNotificationViewExpanded(container: LinearLayout) {
        val notif = currentState.notification

        val titleText = TextView(context).apply {
            text = notif.title
            setTextColor(Color.WHITE)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
        }

        val bodyText = TextView(context).apply {
            text = notif.text
            setTextColor(Color.rgb(174, 174, 178))
            textSize = 14f
            maxLines = 2
        }

        container.addView(titleText)
        container.addView(bodyText)

        notif.actions.take(2).forEach { action ->
            container.addView(createAppleButton(action.title) {
                try {
                    action.intent?.send()
                    action.onClick?.invoke()
                } catch (_: android.app.PendingIntent.CanceledException) {
                    // The source app may have withdrawn this action.
                }
            })
        }
        container.addView(createAppleButton("Dismiss") {
            notif.onDismiss?.invoke()
        })
    }

    private fun buildBatteryViewExpanded(container: LinearLayout) {
        val bat = currentState.battery

        val batText = TextView(context).apply {
            text = if (bat.isCharging) "⚡ ${bat.percentage}% (${bat.plugType})" else "🔋 ${bat.percentage}%"
            setTextColor(if (bat.isLowBattery) Color.rgb(255, 59, 48) else Color.rgb(52, 199, 89))
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
        }

        val statusText = TextView(context).apply {
            text = if (bat.isCharging) "Charging active" else if (bat.isLowBattery) "Low Battery" else "Battery Healthy"
            setTextColor(Color.rgb(174, 174, 178))
            textSize = 14f
        }

        container.addView(batText)
        container.addView(statusText)
    }

    private fun buildDownloadViewExpanded(container: LinearLayout) {
        val dl = currentState.download

        val titleText = TextView(context).apply {
            text = dl.fileName
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
        }

        val progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            progress = dl.percentage
            max = 100
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(6)
            ).apply {
                setMargins(0, dp(8), 0, dp(8))
            }
        }

        val statusText = TextView(context).apply {
            text = if (dl.isCompleted) "Download complete" else "${dl.percentage}% downloaded"
            setTextColor(Color.rgb(174, 174, 178))
            textSize = 13f
        }

        container.addView(titleText)
        container.addView(progressBar)
        container.addView(statusText)
    }

    private fun buildSystemControlViewExpanded(container: LinearLayout) {
        val sys = currentState.systemControl

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val flashBtn = createAppleButton(if (sys.isFlashlightOn) "Torch ON" else "Torch OFF") {
            sys.onToggleFlashlight?.invoke()
        }

        val ringerText = when (sys.ringerMode) {
            RingerMode.NORMAL -> "Normal"
            RingerMode.VIBRATE -> "Vibrate"
            RingerMode.SILENT -> "Silent"
        }
        val ringerBtn = createAppleButton(ringerText) {
            sys.onToggleRinger?.invoke()
        }

        row.addView(flashBtn)
        row.addView(ringerBtn)

        container.addView(row)
    }

    private fun buildIdleViewExpanded(container: LinearLayout) {
        val titleText = TextView(context).apply {
            text = "Snoop Island"
            setTextColor(Color.WHITE)
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
        }
        val descText = TextView(context).apply {
            text = "Swipe to switch activities • Drag to align around camera"
            setTextColor(Color.rgb(174, 174, 178))
            textSize = 13f
            setPadding(0, dp(4), 0, 0)
        }

        container.addView(titleText)
        container.addView(descText)
    }

    private fun createAppleButton(label: String, onClick: () -> Unit): TextView {
        return TextView(context).apply {
            text = label
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(8), dp(16), dp(8))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(dp(6), 0, dp(6), 0)
            }
            background = GradientDrawable().apply {
                setColor(Color.rgb(44, 44, 46))
                cornerRadius = dp(16).toFloat()
            }
            setOnClickListener { onClick() }
        }
    }

    private fun getCollapsedTitle(): String {
        return when (currentState.activeModule) {
            ModuleType.MUSIC -> currentState.music.title
            ModuleType.CALL -> currentState.call.callerName
            ModuleType.TIMER -> {
                val rem = currentState.timer.remainingSeconds
                String.format("%02d:%02d", rem / 60, rem % 60)
            }
            ModuleType.STOPWATCH -> {
                val secs = currentState.stopwatch.elapsedMillis / 1000
                String.format("%02d:%02d", secs / 60, secs % 60)
            }
            ModuleType.NAVIGATION -> currentState.navigation.distanceText
            ModuleType.NOTIFICATION -> currentState.notification.title
            ModuleType.BATTERY -> "${currentState.battery.percentage}%"
            ModuleType.DOWNLOAD -> "${currentState.download.percentage}%"
            ModuleType.SYSTEM_CONTROL -> if (currentState.systemControl.isFlashlightOn) "Torch ON" else "Snoop"
            ModuleType.IDLE -> "Snoop"
        }
    }

    private fun getModuleHeaderTitle(module: ModuleType): String {
        return when (module) {
            ModuleType.MUSIC -> "NOW PLAYING"
            ModuleType.CALL -> "PHONE CALL"
            ModuleType.TIMER -> "TIMER"
            ModuleType.STOPWATCH -> "STOPWATCH"
            ModuleType.NAVIGATION -> "NAVIGATION"
            ModuleType.NOTIFICATION -> "NOTIFICATION"
            ModuleType.BATTERY -> "BATTERY"
            ModuleType.DOWNLOAD -> "DOWNLOAD"
            ModuleType.SYSTEM_CONTROL -> "SYSTEM"
            ModuleType.IDLE -> "SNOOP"
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Always feed the detector, but do not return early: the same gesture
        // stream also drives long-press dragging below.
        gestureDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                initialParamX = windowParams.x
                initialParamY = windowParams.y
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDraggingPosition) {
                    val deltaX = (event.rawX - initialTouchX).roundToInt()
                    val deltaY = (event.rawY - initialTouchY).roundToInt()
                    windowParams.x = initialParamX + deltaX
                    windowParams.y = initialParamY + deltaY
                    try {
                        windowManager.updateViewLayout(this, windowParams)
                    } catch (_: Exception) {
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDraggingPosition) {
                    isDraggingPosition = false
                    if (currentSettings.showAlignmentGuide) {
                        mainPillBackground.setStroke(dp(2), Color.argb(220, 255, 59, 48))
                    } else {
                        mainPillBackground.setStroke(0, Color.TRANSPARENT)
                    }
                    invalidate()
                    stateHub.settingsManager.updateSettings {
                        it.copy(offsetXDp = pxToDp(windowParams.x), offsetYDp = pxToDp(windowParams.y))
                    }
                }
            }
        }
        return true
    }

    private fun dp(value: Int): Int = (value * density).roundToInt()
    private fun pxToDp(px: Int): Int = (px / density).roundToInt()
}
