package com.aminmart.moneymanager.domain.usecase

import com.aminmart.moneymanager.domain.repository.ExportRepository

/**
 * Use case to export transactions to CSV
 */
class ExportToCsvUseCase(
    private val repository: ExportRepository
) {
    suspend operator fun invoke(
        destinationPath: String,
        startDate: Long? = null,
        endDate: Long? = null
    ): String? {
        return repository.exportToCsv(destinationPath, startDate, endDate)
    }
}

/**
 * Use case to export transactions to Excel
 */
class ExportToExcelUseCase(
    private val repository: ExportRepository
) {
    suspend operator fun invoke(
        destinationPath: String,
        startDate: Long? = null,
        endDate: Long? = null
    ): String? {
        return repository.exportToExcel(destinationPath, startDate, endDate)
    }
}

/**
 * Use case to export report
 */
class ExportReportUseCase(
    private val repository: ExportRepository
) {
    suspend operator fun invoke(
        destinationPath: String,
        format: ExportRepository.ExportFormat
    ): String? {
        return repository.exportReport(destinationPath, format)
    }
}
