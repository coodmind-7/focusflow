package com.focusflow.app.domain.repository

import com.focusflow.app.domain.model.TaskLabel
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun getTasks(): Flow<List<TaskLabel>>
    suspend fun insertTask(task: TaskLabel): Long
    suspend fun updateTask(task: TaskLabel)
    suspend fun deleteTask(task: TaskLabel)
    suspend fun reorderTasks(tasks: List<TaskLabel>)
    suspend fun getAllTasksOnce(): List<TaskLabel>
    suspend fun deleteAllTasks()
    suspend fun insertAllTasks(tasks: List<TaskLabel>)
}
