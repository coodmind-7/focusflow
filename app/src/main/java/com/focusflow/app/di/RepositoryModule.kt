package com.focusflow.app.di

import com.focusflow.app.data.repository.TaskRepositoryImpl
import com.focusflow.app.data.repository.TimerRepositoryImpl
import com.focusflow.app.domain.repository.TaskRepository
import com.focusflow.app.domain.repository.TimerRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository

    @Binds
    abstract fun bindTimerRepository(impl: TimerRepositoryImpl): TimerRepository
}
