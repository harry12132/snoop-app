package com.harry12132.snoop.island

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harry12132.snoop.model.ModuleType
import com.harry12132.snoop.model.SnoopOverlayState

@Composable
fun CompactRow(
    h: Dp = 32.dp,
    gap: Dp = 36.dp,
    left: @Composable BoxScope.() -> Unit,
    right: @Composable BoxScope.() -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.padding(start = h * 0.35f),
            contentAlignment = Alignment.CenterStart,
            content = left
        )
        Spacer(Modifier.width(gap))
        Box(
            modifier = Modifier.padding(end = h * 0.35f),
            contentAlignment = Alignment.CenterEnd,
            content = right
        )
    }
}

@Composable
fun CompactIslandContent(
    state: SnoopOverlayState,
    cameraGapDp: Dp = 36.dp
) {
    when (state.activeModule) {
        ModuleType.MUSIC -> {
            CompactRow(
                gap = cameraGapDp,
                left = {
                    if (state.music.artwork != null) {
                        Image(
                            bitmap = state.music.artwork.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                    } else {
                        Icon(Ios.music, contentDescription = null, tint = Color(0xFFFF2D55), modifier = Modifier.size(18.dp))
                    }
                },
                right = {
                    if (state.music.isPlaying) {
                        ComposeEqualizer(color = Color(0xFF34C759), modifier = Modifier.size(18.dp, 14.dp))
                    } else {
                        Icon(Ios.music, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
            )
        }
        ModuleType.CALL -> {
            val mins = state.call.durationSeconds / 60
            val secs = state.call.durationSeconds % 60
            val timeStr = String.format("%02d:%02d", mins, secs)
            CompactRow(
                gap = cameraGapDp,
                left = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Ios.call, contentDescription = null, tint = Color(0xFF34C759), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(text = timeStr, color = Color(0xFF34C759), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                },
                right = {
                    ComposeEqualizer(color = Color(0xFF34C759), modifier = Modifier.size(18.dp, 14.dp))
                }
            )
        }
        ModuleType.TIMER -> {
            val rem = state.timer.remainingSeconds
            val timeStr = String.format("%02d:%02d", rem / 60, rem % 60)
            CompactRow(
                gap = cameraGapDp,
                left = {
                    Icon(Ios.timer, contentDescription = null, tint = Color(0xFFFF9F0A), modifier = Modifier.size(18.dp))
                },
                right = {
                    Text(text = timeStr, color = Color(0xFFFF9F0A), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            )
        }
        ModuleType.STOPWATCH -> {
            val secs = state.stopwatch.elapsedMillis / 1000
            val timeStr = String.format("%02d:%02d", secs / 60, secs % 60)
            CompactRow(
                gap = cameraGapDp,
                left = {
                    Icon(Ios.stopwatch, contentDescription = null, tint = Color(0xFFFF9F0A), modifier = Modifier.size(18.dp))
                },
                right = {
                    Text(text = timeStr, color = Color(0xFFFF9F0A), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            )
        }
        ModuleType.BATTERY -> {
            CompactRow(
                gap = cameraGapDp,
                left = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Ios.flash, contentDescription = null, tint = Color(0xFF34C759), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(text = "Charging", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                },
                right = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "${state.battery.percentage}%", color = Color(0xFF34C759), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(4.dp))
                        Icon(Ios.batteryCharging, contentDescription = null, tint = Color(0xFF34C759), modifier = Modifier.size(20.dp, 12.dp))
                    }
                }
            )
        }
        ModuleType.NAVIGATION -> {
            CompactRow(
                gap = cameraGapDp,
                left = {
                    Icon(Ios.navigate, contentDescription = null, tint = Color(0xFF0A84FF), modifier = Modifier.size(18.dp))
                },
                right = {
                    Text(text = state.navigation.distanceText, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            )
        }
        ModuleType.NOTIFICATION -> {
            CompactRow(
                gap = cameraGapDp,
                left = {
                    Icon(Ios.bell, contentDescription = null, tint = Color(0xFFFF3B30), modifier = Modifier.size(18.dp))
                },
                right = {
                    Text(text = state.notification.appName, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            )
        }
        else -> {
            // Idle empty capsule wrapping camera
            Box(modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
fun SecondaryBubbleContent(
    module: ModuleType,
    state: SnoopOverlayState
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        when (module) {
            ModuleType.MUSIC -> {
                if (state.music.artwork != null) {
                    Image(
                        bitmap = state.music.artwork.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Icon(Ios.music, contentDescription = null, tint = Color(0xFFFF2D55), modifier = Modifier.size(16.dp))
                }
            }
            ModuleType.TIMER -> {
                Icon(Ios.timer, contentDescription = null, tint = Color(0xFFFF9F0A), modifier = Modifier.size(16.dp))
            }
            ModuleType.CALL -> {
                Icon(Ios.call, contentDescription = null, tint = Color(0xFF34C759), modifier = Modifier.size(16.dp))
            }
            ModuleType.BATTERY -> {
                Icon(Ios.flash, contentDescription = null, tint = Color(0xFF34C759), modifier = Modifier.size(14.dp))
            }
            else -> {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0A84FF))
                )
            }
        }
    }
}

@Composable
fun ComposeEqualizer(
    color: Color,
    modifier: Modifier = Modifier
) {
    val animFactor = remember { Animatable(0.2f) }

    LaunchedEffect(Unit) {
        animFactor.animateTo(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 450, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    Canvas(modifier = modifier) {
        val numBars = 4
        val gap = 2.5.dp.toPx()
        val barWidth = (size.width - (numBars - 1) * gap) / numBars
        val f = animFactor.value

        val heights = floatArrayOf(
            0.3f + 0.6f * f,
            0.9f - 0.7f * f,
            0.4f + 0.5f * f,
            0.8f - 0.6f * f
        )

        for (i in 0 until numBars) {
            val left = i * (barWidth + gap)
            val h = size.height * heights[i]
            val top = size.height - h
            drawRoundRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
