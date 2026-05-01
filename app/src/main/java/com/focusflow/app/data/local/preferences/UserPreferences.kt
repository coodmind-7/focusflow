package com.focusflow.app.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val dailyGoalSeconds: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[GOAL_KEY] ?: DEFAULT_GOAL_SECONDS
    }

    suspend fun setDailyGoalSeconds(seconds: Long) {
        context.dataStore.edit { prefs ->
            prefs[GOAL_KEY] = seconds
        }
    }

    companion object {
        private val GOAL_KEY = longPreferencesKey("daily_goal_seconds")
        const val DEFAULT_GOAL_SECONDS = 8 * 3600L
    }
}
