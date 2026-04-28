package com.wifimapper.presentation.map.components

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.wifimapper.presentation.map.MapViewport
import com.wifimapper.presentation.map.MeasurementUi
import com.wifimapper.presentation.map.TrajectoryPointUi
import com.wifimapper.presentation.map.rssiToColor

@Composable
fun HeatmapCanvas(
    measurements: List<MeasurementUi>,
    trajectory: List<TrajectoryPointUi>,
    currentPosition: Offset?,
    modifier: Modifier = Modifier,
    initialViewport: MapViewport = MapViewport(),
    isManualMode: Boolean = false,
    onTap: (Offset) -> Unit = {}
) {
    var viewport by remember { mutableStateOf(initialViewport) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var hasUserInteracted by remember { mutableStateOf(false) }

    // Auto-center on first trajectory point or current position if not yet interacted
    if (!hasUserInteracted && canvasSize != IntSize.Zero) {
        val target = currentPosition ?: trajectory.firstOrNull()?.let { Offset(it.x, it.y) }
        if (target != null) {
            val targetOffsetX = canvasSize.width / 2f - target.x * viewport.scale
            val targetOffsetY = canvasSize.height / 2f - target.y * viewport.scale
            if (viewport.offsetX != targetOffsetX || viewport.offsetY != targetOffsetY) {
                Log.d("HeatmapCanvas", "Auto-centering on target=($target), viewport=($targetOffsetX, $targetOffsetY)")
                viewport = viewport.copy(
                    offsetX = targetOffsetX,
                    offsetY = targetOffsetY
                )
            }
        }
    }

    // Auto-follow current position in manual mode (even if user interacted)
    if (isManualMode && currentPosition != null && canvasSize != IntSize.Zero) {
        val targetOffsetX = canvasSize.width / 2f - currentPosition.x * viewport.scale
        val targetOffsetY = canvasSize.height / 2f - currentPosition.y * viewport.scale
        if (kotlin.math.abs(viewport.offsetX - targetOffsetX) > 1f ||
            kotlin.math.abs(viewport.offsetY - targetOffsetY) > 1f
        ) {
            Log.d("HeatmapCanvas", "Auto-follow manual mode position: $currentPosition")
            viewport = viewport.copy(
                offsetX = targetOffsetX,
                offsetY = targetOffsetY
            )
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = it }
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    hasUserInteracted = true
                    val newScale = (viewport.scale * zoom).coerceIn(10f, 500f)
                    val scaleRatio = newScale / viewport.scale

                    val newOffsetX = centroid.x - (centroid.x - viewport.offsetX) * scaleRatio + pan.x
                    val newOffsetY = centroid.y - (centroid.y - viewport.offsetY) * scaleRatio + pan.y

                    viewport = viewport.copy(
                        scale = newScale,
                        offsetX = newOffsetX,
                        offsetY = newOffsetY
                    )
                }
            }
            .pointerInput(isManualMode, viewport) {
                if (isManualMode) {
                    detectTapGestures { tapOffset ->
                        val worldX = (tapOffset.x - viewport.offsetX) / viewport.scale
                        val worldY = (tapOffset.y - viewport.offsetY) / viewport.scale
                        Log.d("HeatmapCanvas", "Tap at screen=($tapOffset) -> world=($worldX, $worldY)")
                        onTap(Offset(worldX, worldY))
                    }
                }
            }
    ) {
        drawGrid(viewport)
        drawHeatmap(measurements, viewport)
        drawTrajectory(trajectory, viewport)
        currentPosition?.let {
            Log.d("HeatmapCanvas", "Drawing current position: $it, screen=(${it.x * viewport.scale + viewport.offsetX}, ${it.y * viewport.scale + viewport.offsetY})")
            drawCurrentPosition(it, viewport)
        }
    }
}

private fun DrawScope.drawGrid(viewport: MapViewport) {
    val gridSpacingMeters = when {
        viewport.scale > 100f -> 0.5f
        viewport.scale > 50f -> 1f
        viewport.scale > 20f -> 2f
        else -> 5f
    }

    val gridSpacingPixels = gridSpacingMeters * viewport.scale
    val startX = viewport.offsetX % gridSpacingPixels
    val startY = viewport.offsetY % gridSpacingPixels

    val gridColor = Color.Gray.copy(alpha = 0.2f)

    // Vertical lines
    var x = startX
    while (x < size.width) {
        if (x >= 0) {
            drawLine(
                color = gridColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1f
            )
        }
        x += gridSpacingPixels
    }

    // Horizontal lines
    var y = startY
    while (y < size.height) {
        if (y >= 0) {
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
        }
        y += gridSpacingPixels
    }

    // Axes
    drawLine(
        color = Color.Gray.copy(alpha = 0.5f),
        start = Offset(viewport.offsetX, 0f),
        end = Offset(viewport.offsetX, size.height),
        strokeWidth = 2f
    )
    drawLine(
        color = Color.Gray.copy(alpha = 0.5f),
        start = Offset(0f, viewport.offsetY),
        end = Offset(size.width, viewport.offsetY),
        strokeWidth = 2f
    )
}

private fun DrawScope.drawHeatmap(measurements: List<MeasurementUi>, viewport: MapViewport) {
    if (measurements.isEmpty()) return

    // Draw each measurement as a colored circle with gradient
    measurements.forEach { measurement ->
        val screenX = measurement.x * viewport.scale + viewport.offsetX
        val screenY = measurement.y * viewport.scale + viewport.offsetY
        val radius = viewport.scale * 0.8f // 0.8 meter radius

        val color = rssiToColor(measurement.rssiDbm)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    color.copy(alpha = 0.6f),
                    color.copy(alpha = 0.1f),
                    Color.Transparent
                ),
                center = Offset(screenX, screenY),
                radius = radius
            ),
            radius = radius,
            center = Offset(screenX, screenY)
        )

        // Draw center dot
        drawCircle(
            color = color.copy(alpha = 0.9f),
            radius = 4f,
            center = Offset(screenX, screenY)
        )
    }
}

private fun DrawScope.drawTrajectory(trajectory: List<TrajectoryPointUi>, viewport: MapViewport) {
    if (trajectory.size < 2) return

    val path = Path().apply {
        val first = trajectory.first()
        moveTo(
            first.x * viewport.scale + viewport.offsetX,
            first.y * viewport.scale + viewport.offsetY
        )
        trajectory.drop(1).forEach { point ->
            lineTo(
                point.x * viewport.scale + viewport.offsetX,
                point.y * viewport.scale + viewport.offsetY
            )
        }
    }

    drawPath(
        path = path,
        color = Color.Blue.copy(alpha = 0.7f),
        style = Stroke(width = 3f)
    )

    // Draw start point
    val start = trajectory.first()
    drawCircle(
        color = Color.Green,
        radius = 6f,
        center = Offset(
            start.x * viewport.scale + viewport.offsetX,
            start.y * viewport.scale + viewport.offsetY
        )
    )
}

private fun DrawScope.drawCurrentPosition(position: Offset, viewport: MapViewport) {
    val screenX = position.x * viewport.scale + viewport.offsetX
    val screenY = position.y * viewport.scale + viewport.offsetY

    // Outer ring
    drawCircle(
        color = Color.Cyan.copy(alpha = 0.4f),
        radius = 16f,
        center = Offset(screenX, screenY)
    )

    // Inner dot
    drawCircle(
        color = Color.Cyan,
        radius = 8f,
        center = Offset(screenX, screenY)
    )

    // Direction indicator
    drawLine(
        color = Color.Cyan,
        start = Offset(screenX, screenY),
        end = Offset(screenX, screenY - 24f),
        strokeWidth = 3f
    )
}
