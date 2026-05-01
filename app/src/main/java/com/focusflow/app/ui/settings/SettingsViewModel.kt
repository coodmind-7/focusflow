package com.focusflow.app.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.focusflow.app.data.backup.BackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class CategoryToggles(
    val tasks: Boolean = true,
    val timerRecords: Boolean = true,
    val dailyGoals: Boolean = true,
    val settings: Boolean = true
)

data class SettingsUiState(
    val exportCategories: CategoryToggles = CategoryToggles(),
    val importCategories: CategoryToggles = CategoryToggles(),
    val isProcessing: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val backupManager: BackupManager
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun toggleExportTasks() { _uiState.update { it.copy(exportCategories = it.exportCategories.copy(tasks = !it.exportCategories.tasks)) } }
    fun toggleExportRecords() { _uiState.update { it.copy(exportCategories = it.exportCategories.copy(timerRecords = !it.exportCategories.timerRecords)) } }
    fun toggleExportGoals() { _uiState.update { it.copy(exportCategories = it.exportCategories.copy(dailyGoals = !it.exportCategories.dailyGoals)) } }
    fun toggleExportSettings() { _uiState.update { it.copy(exportCategories = it.exportCategories.copy(settings = !it.exportCategories.settings)) } }

    fun toggleImportTasks() { _uiState.update { it.copy(importCategories = it.importCategories.copy(tasks = !it.importCategories.tasks)) } }
    fun toggleImportRecords() { _uiState.update { it.copy(importCategories = it.importCategories.copy(timerRecords = !it.importCategories.timerRecords)) } }
    fun toggleImportGoals() { _uiState.update { it.copy(importCategories = it.importCategories.copy(dailyGoals = !it.importCategories.dailyGoals)) } }
    fun toggleImportSettings() { _uiState.update { it.copy(importCategories = it.importCategories.copy(settings = !it.importCategories.settings)) } }

    fun exportToUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, message = null) }
            try {
                val cats = _uiState.value.exportCategories
                val json = backupManager.exportToJson(
                    includeTasks = cats.tasks,
                    includeTimerRecords = cats.timerRecords,
                    includeDailyGoals = cats.dailyGoals,
                    includeSettings = cats.settings
                )
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    } ?: throw IllegalStateException("无法写入文件")
                }
                _uiState.update { it.copy(isProcessing = false, message = "导出成功") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isProcessing = false, message = "导出失败: ${e.message}") }
            }
        }
    }

    fun importFromUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, message = null) }
            try {
                val json = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use { inp ->
                        String(inp.readBytes(), Charsets.UTF_8)
                    } ?: throw IllegalStateException("无法读取文件")
                }
                val cats = _uiState.value.importCategories
                backupManager.importFromJson(
                    jsonString = json,
                    importTasks = cats.tasks,
                    importTimerRecords = cats.timerRecords,
                    importDailyGoals = cats.dailyGoals,
                    importSettings = cats.settings
                )
                _uiState.update { it.copy(isProcessing = false, message = "导入成功") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isProcessing = false, message = "导入失败: ${e.message}") }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
