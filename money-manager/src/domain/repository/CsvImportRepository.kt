package com.aminmart.moneymanager.domain.repository

import com.aminmart.moneymanager.domain.model.Transaction

/**
 * Repository interface for CSV Import operations
 */
interface CsvImportRepository {
    
    /**
     * Parse a CSV file and return transactions
     * @param filePath Path to the CSV file
     * @return List of parsed transactions
     */
    suspend fun parseCsvFile(filePath: String): CsvImportResult
    
    /**
     * Import transactions from CSV file
     * @param filePath Path to the CSV file
     * @param skipDuplicates Whether to skip duplicate transactions
     * @return Import result with statistics
     */
    suspend fun importFromCsv(filePath: String, skipDuplicates: Boolean = true): CsvImportResult
    
    /**
     * Validate CSV file format
     * @param filePath Path to the CSV file
     * @return True if valid CSV format
     */
    suspend fun validateCsvFile(filePath: String): Boolean
    
    /**
     * Get preview of CSV data
     * @param filePath Path to the CSV file
     * @param limit Number of rows to preview
     * @return List of preview transactions
     */
    suspend fun getCsvPreview(filePath: String, limit: Int = 10): List<Transaction>
}

/**
 * Result of CSV import operation
 */
data class CsvImportResult(
    val success: Boolean,
    val importedCount: Int = 0,
    val skippedCount: Int = 0,
    val failedCount: Int = 0,
    val errorMessage: String? = null,
    val transactions: List<Transaction> = emptyList()
)
