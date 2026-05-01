package com.focusflow.app.domain.usecase.task

import com.focusflow.app.domain.model.TaskLabel
import com.focusflow.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTasksUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    operator fun invoke(): Flow<List<TaskLabel>> = repository.getTasks()
}
