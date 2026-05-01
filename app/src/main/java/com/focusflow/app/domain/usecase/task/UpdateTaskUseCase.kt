package com.focusflow.app.domain.usecase.task

import com.focusflow.app.domain.model.TaskLabel
import com.focusflow.app.domain.repository.TaskRepository
import javax.inject.Inject

class UpdateTaskUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(task: TaskLabel) {
        repository.updateTask(task)
    }
}
