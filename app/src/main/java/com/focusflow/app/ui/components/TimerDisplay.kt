package com.focusflow.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusflow.app.domain.model.TimerMode
import com.focusflow.app.service.TimerForegroundService
import com.focusflow.app.ui.theme.hexToColor

@Composable
fun TimerDisplay(
    elapsedMs: Long,
    mode: TimerMode = TimerMode.COUNT_UP,
    targetDuration: Long = 0,
    isRunning: Boolean = false,
    taskName: String = "",
    taskColor: String = "",
    modifier: Modifier = Modifier
) {
    val displayMs = if (mode == TimerMode.COUNT_DOWN) {
        (targetDuration - elapsedMs).coerceAtLeast(0)
    } else {
        elapsedMs
    }
    val timeText = TimerForegroundService.formatTime(displayMs)
    val parts = timeText.split(":")

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isRunning && taskName.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(hexToColor(taskColor))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = taskName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            parts.forEachIndexed { index, part ->
                Text(
                    text = part,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = 4.sp
                )
                if (index < parts.size - 1) {
                    Text(
                        text = ":",
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = 4.sp
                    )
                }
            }
        }

        if (mode == TimerMode.COUNT_DOWN && targetDuration > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "目标: ${TimerForegroundService.formatTime(targetDuration)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
