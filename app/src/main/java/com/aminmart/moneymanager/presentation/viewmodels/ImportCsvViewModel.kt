package com.aminmart.moneymanager.presentation.viewmodels

import com.aminmart.moneymanager.domain.model.Transaction
import com.aminmart.moneymanager.domain.repository.CsvImportResult
import com.aminmart.moneymanager.domain.usecase.GetCsvPreviewUseCase
import com.aminmart.moneymanager.domain.usecase.ImportCsvUseCase
import com.aminmart.moneymanager.domain.usecase.ValidateCsvUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for Import CSV screen
 */
class ImportCsvViewModel(
    private val importCsvUseCase: ImportCsvUseCase,
    private val validateCsvUseCase: ValidateCsvUseCase,
    private val getCsvPreviewUseCase: GetCsvPreviewUseCase
) {

    private val _uiState = MutableStateFlow(ImportCsvUiState())
    val uiState: StateFlow<ImportCsvUiState> = _uiState.asStateFlow()

    private val _preview = MutableStateFlow<List<Transaction>>(emptyList())
    val preview: StateFlow<List<Transaction>> = _preview.asStateFlow()

    private val _importResult = MutableStateFlow<CsvImportResult?>(null)
    val importResult: StateFlow<CsvImportResult?> = _importResult.asStateFlow()

    suspend fun validateCsvFile(filePath: String): Boolean {
        return validateCsvUseCase(filePath)
    }

    suspend fun loadPreview(filePath: String) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        
        try {
            val transactions = getCsvPreviewUseCase(filePath, 20)
            _preview.value = transactions
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = null
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Failed to load preview: ${e.message}"
            )
        }
    }

    suspend fun importCsv(filePath: String) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        
        try {
            val result = importCsvUseCase(filePath, skipDuplicates = true)
            _importResult.value = result
            
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                successMessage = if (result.success) {
                    "Imported ${result.importedCount} transactions"
                } else {
                    result.errorMessage ?: "Import failed"
                },
                error = if (!result.success && result.errorMessage != null) {
                    result.errorMessage
                } else {
                    null
                }
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Import failed: ${e.message}"
            )
        }
    }
}

/**
 * UI State for Import CSV
 */
data class ImportCsvUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)
