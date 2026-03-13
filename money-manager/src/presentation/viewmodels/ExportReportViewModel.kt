package com.aminmart.moneymanager.presentation.viewmodels

import com.aminmart.moneymanager.domain.usecase.ExportToCsvUseCase
import com.aminmart.moneymanager.domain.usecase.ExportToExcelUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for Export Report screen
 */
class ExportReportViewModel(
    private val exportToCsvUseCase: ExportToCsvUseCase,
    private val exportToExcelUseCase: ExportToExcelUseCase,
    private val exportReportUseCase: ExportReportUseCase
) {

    private val _uiState = MutableStateFlow(ExportReportUiState())
    val uiState: StateFlow<ExportReportUiState> = _uiState.asStateFlow()

    suspend fun exportToCsv(
        destinationPath: String,
        startDate: Long? = null,
        endDate: Long? = null
    ): String? {
        return try {
            exportToCsvUseCase(destinationPath, startDate, endDate)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun exportToExcel(
        destinationPath: String,
        startDate: Long? = null,
        endDate: Long? = null
    ): String? {
        return try {
            exportToExcelUseCase(destinationPath, startDate, endDate)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun exportReport(
        destinationPath: String,
        format: com.aminmart.moneymanager.domain.repository.ExportRepository.ExportFormat
    ): String? {
        return try {
            exportReportUseCase(destinationPath, format)
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * UI State for Export Report
 */
data class ExportReportUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)
