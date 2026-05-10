package com.aminmart.moneymanager.presentation.viewmodels

import com.aminmart.moneymanager.domain.model.Transaction
import com.aminmart.moneymanager.domain.repository.BackupRepository
import com.aminmart.moneymanager.domain.usecase.AutoBackupUseCase
import com.aminmart.moneymanager.domain.usecase.CreateBackupUseCase
import com.aminmart.moneymanager.domain.usecase.DeleteBackupUseCase
import com.aminmart.moneymanager.domain.usecase.GetAvailableBackupsUseCase
import com.aminmart.moneymanager.domain.usecase.RestoreBackupUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * ViewModel for Settings screen
 */
class SettingsViewModel(
    private val createBackupUseCase: CreateBackupUseCase,
    private val restoreBackupUseCase: RestoreBackupUseCase,
    private val getAvailableBackupsUseCase: GetAvailableBackupsUseCase,
    private val autoBackupUseCase: AutoBackupUseCase,
    private val deleteBackupUseCase: DeleteBackupUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _backups = MutableStateFlow<List<String>>(emptyList())
    val backups: StateFlow<List<String>> = _backups.asStateFlow()

    private val _autoBackupEnabled = MutableStateFlow(true)
    val autoBackupEnabled: StateFlow<Boolean> = _autoBackupEnabled.asStateFlow()

    private val _backupFormat = MutableStateFlow(BackupFormat.JSON)
    val backupFormat: StateFlow<BackupFormat> = _backupFormat.asStateFlow()

    fun loadBackups() {
        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            try {
                val backupsList = getAvailableBackupsUseCase()
                _backups.value = backupsList
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    suspend fun createBackup(format: BackupRepository.BackupFormat, path: String): String? {
        return createBackupUseCase(format, path)
    }

    suspend fun restoreBackup(path: String): Boolean {
        return restoreBackupUseCase(path)
    }

    suspend fun deleteBackup(path: String): Boolean {
        return deleteBackupUseCase(path)
    }

    suspend fun autoBackup(): String? {
        return autoBackupUseCase()
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        _autoBackupEnabled.value = enabled
    }

    fun setBackupFormat(format: BackupFormat) {
        _backupFormat.value = format
    }

    fun getBackupFolder(): String {
        return android.os.Environment.getExternalStoragePublicDirectory(
            android.os.Environment.DIRECTORY_DOCUMENTS
        ).absolutePath + "/MoneyManagerBackup"
    }
}

/**
 * Backup format options
 */
enum class BackupFormat {
    JSON,
    CSV
}

/**
 * UI State for Settings
 */
data class SettingsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)
