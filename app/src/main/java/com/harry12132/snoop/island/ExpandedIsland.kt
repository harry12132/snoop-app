package com.harry12132.snoop.island

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harry12132.snoop.manager.SnoopStateHub
import com.harry12132.snoop.model.CallStatus
import com.harry12132.snoop.model.ModuleType
import com.harry12132.snoop.model.RingerMode
import com.harry12132.snoop.model.SnoopOverlayState

@Composable
fun ExpandedIslandContent(
    state: SnoopOverlayState,
    stateHub: SnoopStateHub,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val title = when (state.activeModule) {
                ModuleType.MUSIC -> "NOW PLAYING"
                ModuleType.CALL -> "PHONE CALL"
                ModuleType.TIMER -> "TIMER"
                ModuleType.STOPWATCH -> "STOPWATCH"
                ModuleType.NAVIGATION -> "NAVIGATION"
                ModuleType.NOTIFICATION -> "NOTIFICATION"
                ModuleType.BATTERY -> "BATTERY"
                ModuleType.DOWNLOAD -> "DOWNLOAD"
                ModuleType.SYSTEM_CONTROL -> "SYSTEM"
                ModuleType.IDLE -> "SNOOP ISLAND"
            }
            Text(
                text = title,
                color = Color(0xFF8E8E93),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )

            val idx = state.availableModules.indexOf(state.activeModule) + 1
            Text(
                text = "• $idx/${state.availableModules.size} •",
                color = Color(0xFF636366),
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            when (state.activeModule) {
                ModuleType.MUSIC -> MusicExpandedCard(state)
                ModuleType.CALL -> CallExpandedCard(state, stateHub)
                ModuleType.TIMER -> TimerExpandedCard(state, stateHub)
                ModuleType.STOPWATCH -> StopwatchExpandedCard(state, stateHub)
                ModuleType.NAVIGATION -> NavigationExpandedCard(state)
                ModuleType.NOTIFICATION -> NotificationExpandedCard(state)
                ModuleType.BATTERY -> BatteryExpandedCard(state)
                ModuleType.DOWNLOAD -> DownloadExpandedCard(state)
                ModuleType.SYSTEM_CONTROL -> SystemControlExpandedCard(state)
                ModuleType.IDLE -> IdleExpandedCard()
            }
        }
    }
}

@Composable
private fun MusicExpandedCard(state: SnoopOverlayState) {
    val music = state.music

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (music.artwork != null) {
                Image(
                    bitmap = music.artwork.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2C2C2E)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Ios.music, contentDescription = null, tint = Color(0xFFFF2D55), modifier = Modifier.size(28.dp))
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = music.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = music.artist,
                    color = Color(0xFFAEAEB2),
                    fontSize = 14.sp,
                    maxLines = 1
                )
            }

            if (music.isPlaying) {
                ComposeEqualizer(
                    color = Color(0xFF1ED760),
                    modifier = Modifier.size(20.dp, 16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        val progressPct = if (music.durationMs > 0) ((music.positionMs.toFloat() / music.durationMs)) else 0.35f
        LinearProgressIndicator(
            progress = { progressPct.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape),
            color = Color.White,
            trackColor = Color(0xFF3A3A3C)
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Ios.back,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(28.dp)
                    .clickable { music.onSkipPrev?.invoke() }
            )

            Icon(
                imageVector = if (music.isPlaying) Ios.pause else Ios.play,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(34.dp)
                    .clickable { music.onPlayPause?.invoke() }
            )

            Icon(
                imageVector = Ios.forward,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(28.dp)
                    .clickable { music.onSkipNext?.invoke() }
            )

            Icon(
                imageVector = Ios.airplay,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun CallExpandedCard(state: SnoopOverlayState, stateHub: SnoopStateHub) {
    val call = state.call

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Ios.call, contentDescription = null, tint = Color(0xFF34C759), modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                text = call.callerName,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(4.dp))

        val mins = call.durationSeconds / 60
        val secs = call.durationSeconds % 60
        val timeStr = String.format("%02d:%02d", mins, secs)
        val subtitle = when (call.status) {
            CallStatus.INCOMING -> "Incoming Call..."
            CallStatus.OUTGOING -> "Calling..."
            CallStatus.ACTIVE -> "Active Call • $timeStr"
            CallStatus.ENDED -> "Call Ended"
        }

        Text(
            text = subtitle,
            color = Color(0xFFAEAEB2),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            if (call.status == CallStatus.INCOMING) {
                AppleBadgeButton("Decline", Color(0xFFFF3B30)) { call.onReject?.invoke() }
                AppleBadgeButton("Answer", Color(0xFF34C759)) { call.onAnswer?.invoke() }
            } else {
                AppleBadgeButton("End Call", Color(0xFFFF3B30)) {
                    stateHub.callObserver.setSimulatedCall(CallStatus.ENDED)
                }
            }
        }
    }
}

@Composable
private fun TimerExpandedCard(state: SnoopOverlayState, stateHub: SnoopStateHub) {
    val timer = state.timer
    val mins = timer.remainingSeconds / 60
    val secs = timer.remainingSeconds % 60
    val timeStr = String.format("%02d:%02d", mins, secs)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Ios.timer, contentDescription = null, tint = Color(0xFFFF9F0A), modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                text = if (timer.isCompleted) "00:00" else timeStr,
                color = if (timer.isCompleted) Color(0xFFFF3B30) else Color(0xFFFF9F0A),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = timer.label,
            color = Color(0xFFAEAEB2),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AppleBadgeButton(if (timer.isRunning) "Pause" else "Start", Color(0xFF2C2C2E)) {
                stateHub.timerStopwatchEngine.toggleTimerPause()
            }
            AppleBadgeButton("Reset", Color(0xFF2C2C2E)) {
                stateHub.timerStopwatchEngine.resetTimer()
            }
            AppleBadgeButton("+1m", Color(0xFF2C2C2E)) {
                stateHub.timerStopwatchEngine.addTimerSeconds(60)
            }
        }
    }
}

@Composable
private fun StopwatchExpandedCard(state: SnoopOverlayState, stateHub: SnoopStateHub) {
    val sw = state.stopwatch
    val totalSecs = sw.elapsedMillis / 1000
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    val millis = (sw.elapsedMillis % 1000) / 10
    val timeStr = String.format("%02d:%02d.%02d", mins, secs, millis)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Ios.stopwatch, contentDescription = null, tint = Color(0xFFFF9F0A), modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                text = timeStr,
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AppleBadgeButton(if (sw.isRunning) "Pause" else "Start", Color(0xFF2C2C2E)) {
                stateHub.timerStopwatchEngine.toggleStopwatchPause()
            }
            AppleBadgeButton("Lap", Color(0xFF2C2C2E)) {
                stateHub.timerStopwatchEngine.recordLap()
            }
            AppleBadgeButton("Reset", Color(0xFF2C2C2E)) {
                stateHub.timerStopwatchEngine.resetStopwatch()
            }
        }
    }
}

@Composable
private fun NavigationExpandedCard(state: SnoopOverlayState) {
    val nav = state.navigation
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Ios.navigate, contentDescription = null, tint = Color(0xFF0A84FF), modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text(text = nav.instruction, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        Text(text = "In ${nav.distanceText} • ETA ${nav.etaText}", color = Color(0xFFAEAEB2), fontSize = 14.sp)
    }
}

@Composable
private fun NotificationExpandedCard(state: SnoopOverlayState) {
    val notif = state.notification
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Ios.bell, contentDescription = null, tint = Color(0xFFFF3B30), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(text = notif.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        Text(text = notif.text, color = Color(0xFFAEAEB2), fontSize = 14.sp, maxLines = 2)
    }
}

@Composable
private fun BatteryExpandedCard(state: SnoopOverlayState) {
    val bat = state.battery
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Ios.flash, contentDescription = null, tint = Color(0xFF34C759), modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${bat.percentage}% (${bat.plugType})",
                color = if (bat.isLowBattery) Color(0xFFFF3B30) else Color(0xFF34C759),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (bat.isCharging) "Charging active" else if (bat.isLowBattery) "Low Battery" else "Battery Healthy",
            color = Color(0xFFAEAEB2),
            fontSize = 14.sp
        )
    }
}

@Composable
private fun DownloadExpandedCard(state: SnoopOverlayState) {
    val dl = state.download
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Ios.download, contentDescription = null, tint = Color(0xFF0A84FF), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(text = dl.fileName, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { (dl.percentage.toFloat() / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = Color(0xFF0A84FF),
            trackColor = Color(0xFF3A3A3C)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = if (dl.isCompleted) "Download complete" else "${dl.percentage}% downloaded", color = Color(0xFFAEAEB2), fontSize = 13.sp)
    }
}

@Composable
private fun SystemControlExpandedCard(state: SnoopOverlayState) {
    val sys = state.systemControl
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        AppleBadgeButton(if (sys.isFlashlightOn) "Torch ON" else "Torch OFF", Color(0xFF2C2C2E)) {
            sys.onToggleFlashlight?.invoke()
        }
        val ringerText = when (sys.ringerMode) {
            RingerMode.NORMAL -> "Normal"
            RingerMode.VIBRATE -> "Vibrate"
            RingerMode.SILENT -> "Silent"
        }
        AppleBadgeButton(ringerText, Color(0xFF2C2C2E)) {
            sys.onToggleRinger?.invoke()
        }
    }
}

@Composable
private fun IdleExpandedCard() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Snoop Island", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(text = "Swipe to switch activities • Drag to align around camera", color = Color(0xFFAEAEB2), fontSize = 13.sp)
    }
}

@Composable
private fun AppleBadgeButton(
    label: String,
    bgColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
