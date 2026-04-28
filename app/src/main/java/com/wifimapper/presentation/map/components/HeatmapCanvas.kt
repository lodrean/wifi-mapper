package com.wifimapper.presentation.map.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.platform.LocalDensity
import com.wifimapper.presentation.map.MapViewport
import com.wifimapper.presentation.map.MeasurementUi
import com.wifimapper.presentation.map.TrajectoryPointUi
import com.wifimapper.presentation.map.rssiToColor
import kotlin.math.abs

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
    var isDragging by remember { mutableStateOf(false) }

    // Auto-center on first trajectory point
    if (trajectory.isNotEmpty() && viewport.offsetX == 0f && viewport.offsetY == 0f) {
        val first = trajectory.first()
        viewport = viewport.copy(
            offsetX = -first.x * viewport.scale,
            offsetY = -first.y * viewport.scale
        )
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(isManualMode) {
                if (isManualMode) {
                    detectTapGestures { tapOffset ->
                        // Convert screen coordinates to world coordinates
                        val worldX = (tapOffset.x - viewport.offsetX) / viewport.scale
                        val worldY = (tapOffset.y - viewport.offsetY) / viewport.scale
                        onTap(Offset(worldX, worldY))
                    }
                }
            }
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val newScale = (viewport.scale * zoom).coerceIn(10f, 500f)
                    val scaleRatio = newScale / viewport.scale

                    // Adjust offset to zoom around centroid
                    val newOffsetX = centroid.x - (centroid.x - viewport.offsetX) * scaleRatio + pan.x
                    val newOffsetY = centroid.y - (centroid.y - viewport.offsetY) * scaleRatio + pan.y

                    viewport = viewport.copy(
                        scale = newScale,
                        offsetX = newOffsetX,
                        offsetY = newOffsetY
                    )
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false }
                ) { change, dragAmount ->
                    change.consume()
                    viewport = viewport.copy(
                        offsetX = viewport.offsetX + dragAmount.x,
                        offsetY = viewport.offsetY + dragAmount.y
                    )
                }
            }
    ) {
        drawGrid(viewport)
        drawHeatmap(measurements, viewport)
        drawTrajectory(trajectory, viewport)
        currentPosition?.let { drawCurrentPosition(it, viewport) }
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
