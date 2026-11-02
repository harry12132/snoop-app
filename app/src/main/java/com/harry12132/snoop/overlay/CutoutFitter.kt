package com.harry12132.snoop.overlay

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.view.DisplayCutout
import android.view.Gravity
import android.view.WindowManager

/**
 * Reads the real [DisplayCutout] reported by the OS for this specific phone and
 * derives where the island should sit so it hugs the camera hole without ever
 * covering it. This replaces the old manual cutout "presets" which were guesses.
 */
object CutoutFitter {

    data class CutoutGeometry(
        /** True when the OS reports a cutout/notch we can align to. */
        val hasCutout: Boolean,
        /** Horizontal center of the camera hole in pixels from the left edge. */
        val cutoutCenterX: Int,
        /** Bottom of the deepest cutout inset in pixels (safe Y for the island top edge). */
        val cutoutBottom: Int,
        /** Height of the cutout area in pixels. */
        val cutoutHeight: Int,
        /** Distance from the nearest screen edge to the hole center, in pixels. */
        val insetFromNearestEdge: Int,
        /** True when the hole is on the left half of the screen. */
        val isLeftSide: Boolean,
        /** True when the hole is visually centered (e.g. centered punch-hole / pill). */
        val isCentered: Boolean,
        /** Side of the screen where status-bar corner inset is largest (which corner the hole is near). */
        val nearestEdge: Edge
    )

    enum class Edge { LEFT, CENTER, RIGHT, NONE }

    /** Human-readable device profile, e.g. "Centered punch-hole", for the settings UI. */
    fun describe(context: Context): String {
        val geo = measure(context)
        if (!geo.hasCutout) return "No cutout detected — free positioning"
        val side = when {
            geo.isCentered -> "Centered"
            geo.isLeftSide -> "Left"
            else -> "Right"
        }
        val dpHeight = dp(geo.cutoutHeight, context).coerceAtLeast(1)
        return "$side cutout detected (${dpHeight}dp tall)"
    }

    fun measure(context: Context): CutoutGeometry {
        val dm = context.resources.displayMetrics
        val cutout: DisplayCutout? = findCutout(context)

        if (cutout == null || cutout.boundingRects.isEmpty()) {
            return CutoutGeometry(
                hasCutout = false,
                cutoutCenterX = dm.widthPixels / 2,
                cutoutBottom = 0,
                cutoutHeight = 0,
                insetFromNearestEdge = 0,
                isLeftSide = false,
                isCentered = true,
                nearestEdge = Edge.NONE
            )
        }

        // The camera hole is the top-most bounding rect. Some devices report
        // multiple rects (corner sensors), so pick the one deepest from the top.
        val rect: Rect = cutout.boundingRects.minByOrNull { it.top } ?: cutout.boundingRects[0]
        val centerX = rect.centerX()
        val isCentered = (centerX - dm.widthPixels / 2.0).let { offset ->
            kotlin.math.abs(offset) < dm.widthPixels * 0.15
        }
        val isLeftSide = centerX < dm.widthPixels / 2
        val nearestEdge = when {
            isCentered -> Edge.CENTER
            isLeftSide -> Edge.LEFT
            else -> Edge.RIGHT
        }

        return CutoutGeometry(
            hasCutout = true,
            cutoutCenterX = centerX,
            cutoutBottom = rect.bottom,
            cutoutHeight = rect.height(),
            insetFromNearestEdge = if (isCentered) centerX else if (isLeftSide) centerX else dm.widthPixels - centerX,
            isLeftSide = isLeftSide,
            isCentered = isCentered,
            nearestEdge = nearestEdge
        )
    }

    /**
     * Finds the cutout either from a window attached to the app context
     * (services get a window with display cutout info via WindowInsets) or by
     * asking WindowManager for the maximum stable insets.
     */
    private fun findCutout(context: Context): DisplayCutout? {
        // On API 28+ we can read the display's cutout directly from WindowManager.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
        return try {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            wm?.maximumWindowMetrics?.windowInsets?.displayCutout
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Computes where the island window should be placed, in pixels, for the
     * current settings + real hardware.
     *
     * The island is anchored by its RIGHT or LEFT edge so it grows outward from
     * the camera hole and never expands on top of it. When collapsed it sits
     * beside the hole; when expanded it grows away from the hole.
     */
    data class Placement(
        val horizontalGravity: Int,
        val x: Int,
        val y: Int
    )

    fun placementFor(
        context: Context,
        collapsedWidthPx: Int,
        yOffsetPx: Int,
        xOffsetPx: Int
    ): Placement {
        val geo = measure(context)
        if (!geo.hasCutout) {
            return Placement(android.view.Gravity.CENTER_HORIZONTAL, xOffsetPx, yOffsetPx)
        }
        val dm = context.resources.displayMetrics
        val gap = dp(10, context)

        return when {
            geo.isCentered -> {
                // Centered hole/pill: anchor island's RIGHT edge just left of the hole.
                val islandRightEdge = geo.cutoutCenterX - gap
                Placement(
                    Gravity.TOP or Gravity.LEFT,
                    islandRightEdge - collapsedWidthPx,
                    yOffsetPx
                )
            }
            geo.isLeftSide -> {
                // Hole on left: anchor island's LEFT edge just right of the hole.
                Placement(
                    Gravity.TOP or Gravity.LEFT,
                    geo.cutoutCenterX + gap,
                    yOffsetPx
                )
            }
            else -> {
                // Hole on right: anchor island's RIGHT edge just left of the hole.
                Placement(
                    Gravity.TOP or Gravity.LEFT,
                    geo.cutoutCenterX - gap - collapsedWidthPx,
                    yOffsetPx
                )
            }
        }
    }

    fun dp(value: Int, context: Context): Int =
        (value * context.resources.displayMetrics.density).toInt()
}
