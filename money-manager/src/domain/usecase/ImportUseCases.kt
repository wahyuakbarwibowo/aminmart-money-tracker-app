package com.aminmart.moneymanager.domain.usecase

import com.aminmart.moneymanager.domain.repository.CsvImportRepository
import com.aminmart.moneymanager.domain.repository.CsvImportResult
import com.aminmart.moneymanager.domain.repository.ImportHistoryRepository
import com.aminmart.moneymanager.domain.model.ImportHistory

/**
 * Use case to import CSV file
 */
class ImportCsvUseCase(
    private val repository: CsvImportRepository,
    private val importHistoryRepository: ImportHistoryRepository
) {
    suspend operator fun invoke(filePath: String, skipDuplicates: Boolean = true): CsvImportResult {
        val result = repository.importFromCsv(filePath, skipDuplicates)
        
        if (result.success) {
            importHistoryRepository.insertImportHistory(
                ImportHistory(
                    fileName = filePath.substringAfterLast('/'),
                    transactionCount = result.importedCount,
                    status = ImportHistory.ImportStatus.SUCCESS
                )
            )
        }
        
        return result
    }
}

/**
 * Use case to validate CSV file
 */
class ValidateCsvUseCase(
    private val repository: CsvImportRepository
) {
    suspend operator fun invoke(filePath: String): Boolean {
        return repository.validateCsvFile(filePath)
    }
}

/**
 * Use case to get CSV preview
 */
class GetCsvPreviewUseCase(
    private val repository: CsvImportRepository
) {
    suspend operator fun invoke(filePath: String, limit: Int = 10) = 
        repository.getCsvPreview(filePath, limit)
}
