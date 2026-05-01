package com.focusflow.app.ui.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusflow.app.domain.model.TaskLabel
import com.focusflow.app.domain.model.TimerMode
import com.focusflow.app.domain.usecase.task.CreateTaskUseCase
import com.focusflow.app.domain.usecase.task.DeleteTaskUseCase
import com.focusflow.app.domain.usecase.task.GetTasksUseCase
import com.focusflow.app.domain.usecase.task.ReorderTasksUseCase
import com.focusflow.app.domain.usecase.task.UpdateTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskUiState(
    val tasks: List<TaskLabel> = emptyList(),
    val showEditDialog: Boolean = false,
    val editingTask: TaskLabel? = null
)

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val getTasksUseCase: GetTasksUseCase,
    private val createTaskUseCase: CreateTaskUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val reorderTasksUseCase: ReorderTasksUseCase
) : ViewModel() {

    private val tasks: StateFlow<List<TaskLabel>> = getTasksUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showEditDialog = MutableStateFlow(false)
    private val _editingTask = MutableStateFlow<TaskLabel?>(null)

    val uiState: StateFlow<TaskUiState> = combine(
        tasks, _showEditDialog, _editingTask
    ) { tasks, showDialog, editingTask ->
        TaskUiState(tasks = tasks, showEditDialog = showDialog, editingTask = editingTask)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TaskUiState())

    fun showCreateDialog() {
        _editingTask.value = null
        _showEditDialog.value = true
    }

    fun showEditDialog(task: TaskLabel) {
        _editingTask.value = task
        _showEditDialog.value = true
    }

    fun dismissDialog() {
        _showEditDialog.value = false
        _editingTask.value = null
    }

    fun saveTask(name: String, color: String, timerMode: TimerMode, durationMinutes: Int?) {
        viewModelScope.launch {
            val editing = _editingTask.value
            if (editing != null) {
                updateTaskUseCase(editing.copy(
                    name = name,
                    color = color,
                    defaultTimerMode = timerMode,
                    defaultDurationMinutes = durationMinutes
                ))
            } else {
                createTaskUseCase(TaskLabel(
                    name = name,
                    color = color,
                    defaultTimerMode = timerMode,
                    defaultDurationMinutes = durationMinutes
                ))
            }
            dismissDialog()
        }
    }

    fun deleteTask(task: TaskLabel) {
        viewModelScope.launch {
            deleteTaskUseCase(task)
        }
    }

    fun moveTaskUp(task: TaskLabel) {
        val currentList = uiState.value.tasks
        val index = currentList.indexOfFirst { it.id == task.id }
        if (index > 0) {
            val newList = currentList.toMutableList()
            newList[index] = currentList[index - 1].also { newList[index - 1] = currentList[index] }
            saveNewOrder(newList)
        }
    }

    fun moveTaskDown(task: TaskLabel) {
        val currentList = uiState.value.tasks
        val index = currentList.indexOfFirst { it.id == task.id }
        if (index < currentList.lastIndex) {
            val newList = currentList.toMutableList()
            newList[index] = currentList[index + 1].also { newList[index + 1] = currentList[index] }
            saveNewOrder(newList)
        }
    }

    private fun saveNewOrder(tasks: List<TaskLabel>) {
        viewModelScope.launch {
            reorderTasksUseCase(tasks)
        }
    }
}
