package com.focusflow.app.ui.task

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.focusflow.app.domain.model.TaskLabel
import com.focusflow.app.domain.model.TimerMode
import com.focusflow.app.ui.components.ColorPicker
import com.focusflow.app.ui.theme.ChartColors
import com.focusflow.app.ui.theme.colorToHex
import com.focusflow.app.ui.theme.hexToColor

private val DURATION_PRESETS = listOf(5, 10, 15, 25, 30, 45, 60)

@Composable
fun TaskEditDialog(
    task: TaskLabel?,
    onDismiss: () -> Unit,
    onSave: (name: String, color: String, timerMode: TimerMode, durationMinutes: Int?) -> Unit
) {
    val isNew = task == null
    var name by remember(task) { mutableStateOf(task?.name ?: "") }
    var selectedColor by remember(task) {
        mutableStateOf(task?.color ?: colorToHex(ChartColors[0]))
    }
    var timerMode by remember(task) {
        mutableStateOf(task?.defaultTimerMode ?: TimerMode.COUNT_UP)
    }
    var durationMinutes by remember(task) {
        mutableStateOf(task?.defaultDurationMinutes)
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "新建任务" else "编辑任务") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("任务名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "计时模式",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = timerMode == TimerMode.COUNT_UP,
                        onClick = { timerMode = TimerMode.COUNT_UP },
                        label = { Text("正计时") },
                        shape = RoundedCornerShape(20.dp)
                    )
                    FilterChip(
                        selected = timerMode == TimerMode.COUNT_DOWN,
                        onClick = { timerMode = TimerMode.COUNT_DOWN },
                        label = { Text("倒计时") },
                        shape = RoundedCornerShape(20.dp)
                    )
                }

                if (timerMode == TimerMode.COUNT_DOWN) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "目标时长（分钟）",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        DURATION_PRESETS.forEach { mins ->
                            FilterChip(
                                selected = durationMinutes == mins,
                                onClick = { durationMinutes = mins },
                                label = { Text("${mins}分") },
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = durationMinutes?.toString() ?: "",
                        onValueChange = { value ->
                            durationMinutes = value.toIntOrNull()
                        },
                        label = { Text("自定义分钟数") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "选择颜色",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                ColorPicker(
                    selectedColor = selectedColor,
                    onColorSelected = { selectedColor = it }
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = {
                    val finalDuration = if (timerMode == TimerMode.COUNT_DOWN) durationMinutes else null
                    onSave(name.trim(), selectedColor, timerMode, finalDuration)
                },
                enabled = name.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
