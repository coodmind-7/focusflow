package com.focusflow.app.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportToUri(it) }
    }

    var showImportConfirm by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            pendingImportUri = it
            showImportConfirm = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Export section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "导出数据",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        CategoryCheckbox(
                            label = "任务",
                            checked = uiState.exportCategories.tasks,
                            onToggle = viewModel::toggleExportTasks
                        )
                        CategoryCheckbox(
                            label = "计时记录",
                            checked = uiState.exportCategories.timerRecords,
                            onToggle = viewModel::toggleExportRecords
                        )
                        CategoryCheckbox(
                            label = "每日目标",
                            checked = uiState.exportCategories.dailyGoals,
                            onToggle = viewModel::toggleExportGoals
                        )
                        CategoryCheckbox(
                            label = "每日目标设置",
                            checked = uiState.exportCategories.settings,
                            onToggle = viewModel::toggleExportSettings
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { exportLauncher.launch("focusflow_backup.json") },
                            enabled = !uiState.isProcessing,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("导出到文件")
                        }
                    }
                }

                // Import section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "导入数据",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        CategoryCheckbox(
                            label = "任务",
                            checked = uiState.importCategories.tasks,
                            onToggle = viewModel::toggleImportTasks
                        )
                        CategoryCheckbox(
                            label = "计时记录",
                            checked = uiState.importCategories.timerRecords,
                            onToggle = viewModel::toggleImportRecords
                        )
                        CategoryCheckbox(
                            label = "每日目标",
                            checked = uiState.importCategories.dailyGoals,
                            onToggle = viewModel::toggleImportGoals
                        )
                        CategoryCheckbox(
                            label = "每日目标设置",
                            checked = uiState.importCategories.settings,
                            onToggle = viewModel::toggleImportSettings
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                importLauncher.launch(arrayOf("application/json", "*/*"))
                            },
                            enabled = !uiState.isProcessing,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("从文件导入")
                        }
                    }
                }
            }
        }

        if (uiState.isProcessing) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }

        if (showImportConfirm && pendingImportUri != null) {
            AlertDialog(
                onDismissRequest = {
                    showImportConfirm = false
                    pendingImportUri = null
                },
                title = { Text("确认导入") },
                text = { Text("导入将覆盖现有数据，此操作不可撤销。是否继续？") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showImportConfirm = false
                            pendingImportUri?.let { viewModel.importFromUri(it) }
                            pendingImportUri = null
                        }
                    ) {
                        Text("继续导入")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showImportConfirm = false
                            pendingImportUri = null
                        }
                    ) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

@Composable
private fun CategoryCheckbox(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}
