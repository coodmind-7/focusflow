package com.focusflow.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.focusflow.app.domain.model.TaskTimeSlice
import com.focusflow.app.ui.theme.hexToColor

@Composable
fun DonutChart(
    slices: List<TaskTimeSlice>,
    totalSeconds: Long,
    modifier: Modifier = Modifier,
    thickness: Dp = 20.dp
) {
    val staggerDelay = 150
    val sectorDuration = 600
    val totalSpan = if (slices.isNotEmpty()) staggerDelay * (slices.size - 1) + sectorDuration else sectorDuration

    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(slices, totalSeconds) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, tween(totalSpan))
    }

    val progress = animProgress.value

    val emptyRingColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = thickness.toPx()
            val diameter = size.minDimension - strokePx
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)
            val arcSize = Size(diameter, diameter)
            val gapAngle = 2f
            val totalGap = if (slices.isNotEmpty()) gapAngle * slices.size else 0f
            val availableAngle = 360f - totalGap

            if (slices.isEmpty() || totalSeconds == 0L) {
                drawArc(
                    color = emptyRingColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            } else {
                var startAngle = -90f
                for ((index, slice) in slices.withIndex()) {
                    val fullSweep = (slice.seconds.toFloat() / totalSeconds.toFloat()) * availableAngle

                    // Sequential stagger: each sector animates from 0 to fullSweep with delayed start
                    val sectorStart = index * staggerDelay.toFloat()
                    val elapsed = progress * totalSpan
                    val sectorProgress = ((elapsed - sectorStart) / sectorDuration.toFloat()).coerceIn(0f, 1f)

                    val sweep = fullSweep * sectorProgress
                    if (sweep > 0.5f) {
                        drawArc(
                            color = hexToColor(slice.color),
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokePx, cap = StrokeCap.Butt)
                        )
                    }
                    startAngle += fullSweep + gapAngle
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "总时长",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatSeconds(totalSeconds),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

fun formatSeconds(totalSeconds: Long): String {
    if (totalSeconds == 0L) return "0m"
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}
