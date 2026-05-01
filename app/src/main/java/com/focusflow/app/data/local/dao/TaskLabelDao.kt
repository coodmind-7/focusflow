package com.focusflow.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.focusflow.app.data.local.entity.TaskLabelEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class TaskLabelDao {

    @Query("SELECT * FROM task_labels ORDER BY sortOrder ASC")
    abstract fun getAllTasks(): Flow<List<TaskLabelEntity>>

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM task_labels")
    abstract suspend fun getMaxSortOrder(): Int

    @Insert
    abstract suspend fun insertTask(task: TaskLabelEntity): Long

    @Update
    abstract suspend fun updateTask(task: TaskLabelEntity)

    @Delete
    abstract suspend fun deleteTask(task: TaskLabelEntity)

    @Query("UPDATE task_labels SET sortOrder = :sortOrder WHERE id = :id")
    abstract suspend fun updateSortOrder(id: Long, sortOrder: Int)

    @Transaction
    open suspend fun reorderTasks(updates: List<Pair<Long, Int>>) {
        updates.forEach { (id, order) ->
            updateSortOrder(id, order)
        }
    }

    @Query("SELECT * FROM task_labels ORDER BY sortOrder ASC")
    abstract suspend fun getAllTasksSuspend(): List<TaskLabelEntity>

    @Query("DELETE FROM task_labels")
    abstract suspend fun deleteAllTasks()

    @Insert
    abstract suspend fun insertAllTasks(tasks: List<TaskLabelEntity>)

    @Transaction
    open suspend fun replaceAllTasks(tasks: List<TaskLabelEntity>): List<Long> {
        deleteAllTasks()
        return tasks.map { insertTask(it) }
    }
}
