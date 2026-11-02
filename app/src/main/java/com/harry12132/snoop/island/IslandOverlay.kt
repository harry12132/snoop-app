package com.harry12132.snoop.island

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.harry12132.snoop.data.SnoopSettingsManager
import com.harry12132.snoop.manager.SnoopStateHub
import com.harry12132.snoop.model.ModuleType

@Composable
fun SnoopIslandOverlay(
    stateHub: SnoopStateHub,
    settingsManager: SnoopSettingsManager
) {
    val settings by settingsManager.settingsFlow.collectAsState()
    val state by stateHub.overlayState.collectAsState()

    var isExpanded by remember { mutableStateOf(false) }

    val islandWidth by animateDpAsState(
        targetValue = if (isExpanded) settings.expandedWidthDp.dp else settings.collapsedWidthDp.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "islandWidth"
    )

    val islandHeight by animateDpAsState(
        targetValue = if (isExpanded) settings.expandedHeightDp.dp else settings.collapsedHeightDp.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "islandHeight"
    )

    val cornerRadius by animateDpAsState(
        targetValue = if (isExpanded) settings.cornerRadiusDp.dp else (settings.collapsedHeightDp / 2).dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "cornerRadius"
    )

    // Check if secondary bubble should appear
    val secondaryModule = if (settings.showSecondaryBubble && state.availableModules.size > 1) {
        state.availableModules.getOrNull(1)
    } else null

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = settings.offsetYDp.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Top
    ) {
        // Main Dynamic Island Capsule
        Box(
            modifier = Modifier
                .width(islandWidth)
                .height(islandHeight)
                .clip(RoundedCornerShape(cornerRadius))
                .background(Color.Black)
                .then(
                    if (settings.showAlignmentGuide) {
                        Modifier.border(2.dp, Color(0xFFFF3B30), RoundedCornerShape(cornerRadius))
                    } else Modifier
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { isExpanded = !isExpanded }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (isExpanded) {
                ExpandedIslandContent(
                    state = state,
                    stateHub = stateHub
                )
            } else {
                CompactIslandContent(
                    state = state
                )
            }
        }

        // Secondary Activity Bubble
        if (!isExpanded && secondaryModule != null) {
            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(settings.collapsedHeightDp.dp)
                    .clip(CircleShape)
                    .background(Color.Black)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { stateHub.cycleNextModule() }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                SecondaryBubbleContent(
                    module = secondaryModule,
                    state = state
                )
            }
        }
    }
}
