package com.focusflow.app.ui.timer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusflow.app.domain.model.TaskLabel
import com.focusflow.app.domain.model.TimerMode
import com.focusflow.app.service.TimerState
import com.focusflow.app.ui.components.DailySummaryBar
import com.focusflow.app.ui.components.TimerDisplay
import com.focusflow.app.ui.task.TaskEditDialog
import com.focusflow.app.ui.theme.hexToColor
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TimerScreen(
    viewModel: TimerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pausedTaskIds by viewModel.pausedTaskIds.collectAsStateWithLifecycle()
    val isRunning = uiState.timerState == TimerState.RUNNING
    val isPaused = uiState.timerState == TimerState.PAUSED

    val runningTask = uiState.tasks.find { it.id == uiState.runningTaskId }

    val bgColor by animateColorAsState(
        targetValue = if (isRunning) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
        } else {
            MaterialTheme.colorScheme.background
        },
        animationSpec = tween(600),
        label = "bgTint"
    )

    LaunchedEffect(uiState.showGoalCelebration) {
        if (uiState.showGoalCelebration) {
            delay(3000)
            viewModel.dismissCelebration()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FocusFlow") },
                actions = {
                    IconButton(onClick = { viewModel.showCreateDialog() }) {
                        Icon(Icons.Filled.Add, contentDescription = "创建任务")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(bgColor)
        ) {
            if (uiState.tasks.isEmpty()) {
                EmptyState(onCreateClick = { viewModel.showCreateDialog() })
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Timer display
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        TimerDisplay(
                            elapsedMs = uiState.elapsedMs,
                            mode = uiState.mode,
                            targetDuration = uiState.targetDuration,
                            isRunning = isRunning,
                            taskName = uiState.runningTaskName,
                            taskColor = runningTask?.color ?: "",
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                    }

                    // Daily summary
                    item {
                        DailySummaryBar(
                            totalSeconds = uiState.todayTotalSeconds,
                            goalSeconds = uiState.dailyGoalSeconds,
                            onGoalClick = { viewModel.showGoalDialog() },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Task rows
                    items(uiState.tasks, key = { it.id }) { task ->
                        val thisActiveRunning =
                            task.id == uiState.runningTaskId && uiState.timerState == TimerState.RUNNING
                        val thisPaused =
                            task.id == uiState.runningTaskId && uiState.timerState == TimerState.PAUSED
                        val hasSession = task.id in pausedTaskIds
                        TaskRow(
                            task = task,
                            isActiveRunning = thisActiveRunning,
                            isPaused = thisPaused,
                            hasSession = hasSession,
                            isLongPressed = uiState.longPressedTaskId == task.id,
                            onPlay = { viewModel.startTimer(task) },
                            onPause = { viewModel.pauseTimer() },
                            onLongPress = { viewModel.toggleLongPress(task.id) },
                            onDismissLongPress = { viewModel.dismissLongPress() },
                            onEdit = { viewModel.showEditDialog(task) },
                            onDelete = { viewModel.deleteTask(task) },
                            onMoveUp = { viewModel.moveTaskUp(task) },
                            onMoveDown = { viewModel.moveTaskDown(task) },
                            onReset = { viewModel.resetTask(task) }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }
        }
    }

    if (uiState.showEditDialog) {
        TaskEditDialog(
            task = uiState.editingTask,
            onDismiss = { viewModel.dismissDialog() },
            onSave = { name, color, mode, duration ->
                viewModel.saveTask(name, color, mode, duration)
            }
        )
    }

    if (uiState.showGoalDialog) {
        GoalEditDialog(
            currentGoalSeconds = uiState.dailyGoalSeconds,
            onDismiss = { viewModel.dismissGoalDialog() },
            onSave = { viewModel.saveGoal(it) }
        )
    }

    if (uiState.showGoalCelebration) {
        GoalCelebrationDialog(onDismiss = { viewModel.dismissCelebration() })
    }
}

@Composable
private fun GoalCelebrationDialog(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = true,
            enter = scaleIn(animationSpec = spring(dampingRatio = 0.6f)) + fadeIn(tween(200)),
            exit = scaleOut() + fadeOut()
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🎉",
                        fontSize = 56.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "今日目标达成!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "继续保持，你做得很好",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("知道了")
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(onCreateClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "还没有任务",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onCreateClick) {
                Text("创建第一个任务")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskRow(
    task: TaskLabel,
    isActiveRunning: Boolean,
    isPaused: Boolean,
    hasSession: Boolean,
    isLongPressed: Boolean,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onLongPress: () -> Unit,
    onDismissLongPress: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onReset: () -> Unit
) {
    val isActive = isActiveRunning || isPaused

    val rowBg by animateColorAsState(
        targetValue = when {
            isActiveRunning -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            else -> MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(300),
        label = "rowBg"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .combinedClickable(
                onClick = onDismissLongPress,
                onLongClick = onLongPress
            ),
        colors = CardDefaults.cardColors(containerColor = rowBg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            AnimatedVisibility(
                visible = isLongPressed && !isActiveRunning,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(200))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hasSession || isPaused) {
                        SmallActionChip(Icons.Filled.Refresh, "重新开始", onReset)
                    }
                    SmallActionChip(Icons.Filled.Edit, "编辑", onEdit)
                    SmallActionChip(Icons.Filled.KeyboardArrowUp, "上移", onMoveUp)
                    SmallActionChip(Icons.Filled.KeyboardArrowDown, "下移", onMoveDown)
                    SmallActionChip(
                        Icons.Filled.Delete, "删除", onDelete,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(hexToColor(task.color))
                )
                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.name,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (task.defaultTimerMode == TimerMode.COUNT_DOWN) {
                            val mins = task.defaultDurationMinutes
                            if (mins != null) "倒计时 · ${mins}分钟" else "倒计时"
                        } else {
                            "正计时"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = {
                        when {
                            isActiveRunning -> onPause()
                            else -> onPlay()
                        }
                    }
                ) {
                    Icon(
                        imageVector = when {
                            isActiveRunning -> Icons.Filled.Pause
                            else -> Icons.Filled.PlayArrow
                        },
                        contentDescription = if (isActiveRunning) "暂停" else "开始",
                        tint = if (isActiveRunning) MaterialTheme.colorScheme.onSurfaceVariant
                               else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun SmallActionChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
        Icon(
            icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
    }
}
