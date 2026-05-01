package com.focusflow.app.data.repository

import com.focusflow.app.data.local.dao.TaskLabelDao
import com.focusflow.app.data.local.entity.toDomain
import com.focusflow.app.data.local.entity.toEntity
import com.focusflow.app.domain.model.TaskLabel
import com.focusflow.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(
    private val dao: TaskLabelDao
) : TaskRepository {

    override fun getTasks(): Flow<List<TaskLabel>> =
        dao.getAllTasks().map { entities -> entities.map { it.toDomain() } }

    override suspend fun insertTask(task: TaskLabel): Long {
        val maxOrder = dao.getMaxSortOrder()
        return dao.insertTask(task.toEntity().copy(sortOrder = maxOrder + 1))
    }

    override suspend fun updateTask(task: TaskLabel) {
        dao.updateTask(task.toEntity())
    }

    override suspend fun deleteTask(task: TaskLabel) {
        dao.deleteTask(task.toEntity())
    }

    override suspend fun reorderTasks(tasks: List<TaskLabel>) {
        val updates = tasks.mapIndexed { index, task ->
            task.id to index
        }
        dao.reorderTasks(updates)
    }

    override suspend fun getAllTasksOnce(): List<TaskLabel> =
        dao.getAllTasksSuspend().map { it.toDomain() }

    override suspend fun deleteAllTasks() = dao.deleteAllTasks()

    override suspend fun insertAllTasks(tasks: List<TaskLabel>) =
        dao.insertAllTasks(tasks.map { it.toEntity() })
}
